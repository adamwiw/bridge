package com.wherecanyoubuy.bridge.scraper.cdp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wherecanyoubuy.bridge.scraper.ScrapedElementInteface;
import com.wherecanyoubuy.bridge.scraper.ScraperInterface;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Chrome DevTools Protocol engine.
 *
 * Launches its own headless Chrome (no ChromeDriver/Selenium), drives it over the CDP
 * WebSocket for navigation, and uses jsoup for DOM queries over the rendered HTML.
 */
public class CdpScraper implements ScraperInterface {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String REAL_UA =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36";
    private static final String STEALTH_JS = """
            Object.defineProperty(navigator, 'webdriver', { get: () => undefined });
            Object.defineProperty(navigator, 'languages', { get: () => ['en-US', 'en'] });
            if (!window.chrome) { window.chrome = { runtime: {} }; }
            """;
    private static final Duration NAV_TIMEOUT = Duration.ofSeconds(45);
    private static final Duration CMD_TIMEOUT = Duration.ofSeconds(20);

    private Process process;
    private Path userDataDir;
    private WebSocket socket;
    private final AtomicLong ids = new AtomicLong();
    private final Map<Long, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();
    private final Set<String> enabledDomains = new HashSet<>();
    private final StringBuilder frame = new StringBuilder();
    private volatile CompletableFuture<Void> loaded = new CompletableFuture<>();
    private volatile int statusCode = 0;
    private volatile Document document = Jsoup.parse("<html></html>");
    private volatile long size = 0;

    @Override
    public void startScraper(String proxy) {
        try {
            userDataDir = profileDir(proxy);
            List<String> cmd = new ArrayList<>(List.of(
                    resolveChrome(),
                    "--headless=new",
                    "--remote-debugging-port=0",
                    "--user-data-dir=" + userDataDir,
                    "--no-first-run",
                    "--no-default-browser-check",
                    "--disable-gpu",
                    "--disable-dev-shm-usage",
                    "--disable-background-networking",
                    "--disable-blink-features=AutomationControlled",
                    "--window-size=1920,1080"
            ));
            if (proxy != null && !proxy.isBlank()) {
                cmd.add("--proxy-server=" + proxy);
            }
            cmd.add("about:blank");

            process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            Thread.ofVirtual().start(() -> {
                try (OutputStream sink = OutputStream.nullOutputStream()) {
                    process.getInputStream().transferTo(sink);
                } catch (Exception ignored) {
                }
            });

            int port = waitForPort();
            connect(firstPageTarget(port));
            call("Page.enable", null);
            call("Network.enable", null);
            call("Page.addScriptToEvaluateOnNewDocument", Map.of("source", STEALTH_JS));
            call("Network.setUserAgentOverride", Map.of(
                    "userAgent", REAL_UA,
                    "acceptLanguage", "en-US,en;q=0.9",
                    "platform", "MacIntel"));
        } catch (Exception e) {
            quit();
            throw new RuntimeException("CDP start failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void getUrl(String url) {
        loaded = new CompletableFuture<>();
        statusCode = 0;
        try {
            call("Page.navigate", Map.of("url", url));
            loaded.get(NAV_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Site unreachable: navigation failed for " + url + " (" + e.getMessage() + ")", e);
        }
        String html = evaluate("document.documentElement ? document.documentElement.outerHTML : ''");
        document = Jsoup.parse(html, url);
        size = html.length();
    }

    @Override
    public List<ScrapedElementInteface> findElements(String cssQuery) {
        return document.select(cssQuery).stream().map(e -> (ScrapedElementInteface) new CdpElement(e)).toList();
    }

    @Override
    public ScrapedElementInteface findElement() {
        return new CdpElement(document.body() == null ? document : document.body());
    }

    @Override
    public String getPageSource() {
        return document.outerHtml();
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    public void quit() {
        try {
            if (socket != null) socket.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
        } catch (Exception ignored) {
        }
        try {
            if (process != null) {
                process.destroy();
                if (!process.waitFor(5, TimeUnit.SECONDS)) process.destroyForcibly();
            }
        } catch (Exception ignored) {
        }
        // The user-data-dir is intentionally kept: it holds the warmed profile
        // (cookies + solved-challenge tokens) for this identity.
    }

    // --- CDP plumbing ---

    private JsonNode call(String method, Map<String, Object> params) {
        long id = ids.incrementAndGet();
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pending.put(id, future);
        try {
            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("id", id);
            payload.put("method", method);
            if (params != null) payload.put("params", params);
            socket.sendText(JSON.writeValueAsString(payload), true).get(CMD_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            return future.get(CMD_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            pending.remove(id);
            throw new RuntimeException("CDP " + method + " failed: " + e.getMessage(), e);
        }
    }

    public String evaluate(String expression) {
        JsonNode response = call("Runtime.evaluate", Map.of(
                "expression", expression,
                "returnByValue", true,
                "awaitPromise", true));
        return response.path("result").path("result").path("value").asText("");
    }

    private void connect(String wsUrl) throws Exception {
        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                frame.append(data);
                if (last) {
                    String message = frame.toString();
                    frame.setLength(0);
                    handle(message);
                }
                webSocket.request(1);
                return null;
            }

            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                pending.values().forEach(f -> f.completeExceptionally(error));
                pending.clear();
            }
        };
        socket = HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .buildAsync(URI.create(wsUrl), listener)
                .get(20, TimeUnit.SECONDS);
    }

    private void handle(String message) {
        try {
            JsonNode node = JSON.readTree(message);
            if (node.has("id")) {
                CompletableFuture<JsonNode> future = pending.remove(node.get("id").asLong());
                if (future != null) future.complete(node.has("error") ? node.get("error") : node);
                return;
            }
            String method = node.path("method").asText("");
            if ("Page.loadEventFired".equals(method)) {
                loaded.complete(null);
            } else if ("Network.responseReceived".equals(method)) {
                JsonNode params = node.path("params");
                if ("Document".equals(params.path("type").asText())) {
                    statusCode = params.path("response").path("status").asInt(0);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private int waitForPort() throws Exception {
        Path portFile = userDataDir.resolve("DevToolsActivePort");
        long deadline = System.currentTimeMillis() + 25_000;
        while (System.currentTimeMillis() < deadline) {
            if (Files.exists(portFile)) {
                List<String> lines = Files.readAllLines(portFile);
                if (!lines.isEmpty() && !lines.get(0).isBlank()) {
                    return Integer.parseInt(lines.get(0).trim());
                }
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("Chrome did not expose a debugging port");
    }

    private String firstPageTarget(int port) throws Exception {
        HttpClient http = HttpClient.newHttpClient();
        for (int attempt = 0; attempt < 50; attempt++) {
            HttpResponse<String> response = http.send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/json/list"))
                            .timeout(Duration.ofSeconds(5)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonNode targets = JSON.readTree(response.body());
            for (JsonNode target : targets) {
                if ("page".equals(target.path("type").asText())) {
                    return target.path("webSocketDebuggerUrl").asText();
                }
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("No page target available");
    }


    private static Path profileDir(String proxy) throws Exception {
        String base = System.getenv("BRIDGE_PROFILE_DIR");
        Path root = base != null && !base.isBlank()
                ? Path.of(base)
                : Path.of(System.getProperty("user.home"), ".bridge", "profiles");
        String key = proxy == null ? "direct" : proxy.replaceAll("[^a-zA-Z0-9.-]", "_");
        Path dir = root.resolve(key);
        Files.createDirectories(dir);
        // Remove leftovers from a previous (possibly killed) run so Chrome can own the profile.
        for (String stale : List.of("SingletonLock", "SingletonSocket", "SingletonCookie", "DevToolsActivePort")) {
            try {
                Files.deleteIfExists(dir.resolve(stale));
            } catch (Exception ignored) {
            }
        }
        return dir;
    }

    private String resolveChrome() {
        String env = System.getenv("CHROME_BIN");
        if (env != null && !env.isBlank()) return env;
        for (String candidate : List.of(
                "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                "/Applications/Chromium.app/Contents/MacOS/Chromium",
                "/usr/bin/google-chrome",
                "/usr/bin/chromium",
                "/usr/bin/chromium-browser")) {
            if (Files.isExecutable(Path.of(candidate))) return candidate;
        }
        return "google-chrome";
    }
}
