package com.wherecanyoubuy.bridge.scraper.selenium;

import com.wherecanyoubuy.bridge.scraper.AbstractScraper;
import com.wherecanyoubuy.bridge.scraper.ScrapedElementInteface;
import lombok.Synchronized;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.io.File;

import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v136.network.Network;
import static org.openqa.selenium.By.ByTagName;

@Deprecated
public class SeleniumScraper extends AbstractScraper {
    private ChromeDriver driver;
    private AtomicLong totalBytes = new AtomicLong(0);

    private int lastStatusCode = -1;

    public SeleniumScraper() {
        super(org.slf4j.LoggerFactory.getLogger(SeleniumScraper.class));
    }

    @Override
    @Synchronized
    public void startScraper(ChromeOptions chromeOptions) {
        chromeOptions.setBinary("/home/adam/bin/chromium-clean");
        chromeOptions.addArguments("--disable-blink-features=AutomationControlled");

        // Setup a Chrome DevTools Protocol (CDP) rule to block certain resource types
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_setting_values.images", 2); // Block images
        prefs.put("profile.managed_default_content_settings.stylesheets", 2); // Block CSS
        prefs.put("profile.managed_default_content_settings.fonts", 2); // Block fonts
        prefs.put("profile.managed_default_content_settings.media_stream", 2); // Block camera/mic
        prefs.put("profile.managed_default_content_settings.plugins", 2); // Block plugins

        chromeOptions.setExperimentalOption("prefs", prefs);
        chromeOptions.addArguments("--disable-plugins");
        chromeOptions.addArguments("--disable-extensions");
        chromeOptions.addArguments("--mute-audio");
        chromeOptions.addArguments("--disable-translate");
        String tempProfile = "/tmp/chrome-profile-clean";
        Path basePath = Paths.get(tempProfile);

        try {
            if (Files.exists(basePath)) {
                Files.walk(basePath)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException e) {
            log.warn("Failed to clean Chrome profile directory: {}", e.getMessage());
        }
        super.startScraper(chromeOptions);
        // Set the system property for Chrome driver
        System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
        // chromeOptions.setHeadless(true);
        driver = new ChromeDriver(chromeOptions);

        DevTools devTools = driver.getDevTools();
        devTools.createSession();
        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
        devTools.send(Network.setBlockedURLs(List.of("*.png", "*.jpg", "*.jpeg", "*.gif", "*.css", "*.woff", "*.woff2", "*.svg", "*.ttf", "*.mp4", "*.webm", "*.avi")));
        devTools.addListener(Network.loadingFinished(), event -> {
            totalBytes.addAndGet(event.getEncodedDataLength().longValue());
        });
        devTools.addListener(Network.responseReceived(), response -> {
            String requestUrl = response
                    .getResponse()
                    .getUrl();
            if (requestUrl.equals(driver.getCurrentUrl())) {
                lastStatusCode = response
                        .getResponse()
                        .getStatus();
            }
        });

        // Create driver object for CHROME browser
        driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
        driver.manage().window().maximize();
    }

    @Override
    @Synchronized
    public void getUrl(String url) throws IOException {
        super.getUrl(url);
        lastStatusCode = -1;
        driver.get(url);
    }

    @Override
    @Synchronized
    public List<ScrapedElementInteface> findElements(String cssQuery) {
        return driver
                .findElements(By.cssSelector(cssQuery))
                .stream()
                .map(webElement ->
                        SeleniumElement
                                .builder()
                                .webElement(webElement)
                                .build())
                .collect(Collectors.toList());
    }

    @Override
    @Synchronized
    public ScrapedElementInteface findElement() {
        return SeleniumElement
                .builder()
                .webElement(driver.findElement(new ByTagName("Body")))
                .build();
    }

    @Override
    public String getPageSource() {
        return driver.getPageSource();
    }

    @Override
    public long getSize() {
        return totalBytes.get();
    }

    @Override
    public int getStatusCode() {
        return lastStatusCode;
    }

    @Override
    @Synchronized
    public void quit() {
        super.quit();
        driver.quit();
    }
}
