# Bridge

**Bridge** is a scalable, headless Java-based web scraping gateway built with Spring Boot and Selenium. It exposes endpoints for scraping dynamic JavaScript-rendered web content using a flexible plugin-like architecture.

## 🚀 Features

- ⚙️ Pluggable scraper engine (Selenium-based with ChromeDriver)
- 🧩 Supports both CSS selector-based (`ElementQuery`) and regex-based (`RegexQuery`) scraping
- 🌐 Chrome DevTools Protocol (CDP) support for bandwidth tracking and resource blocking
- 🔐 Proxy rotation and failure tracking
- 📦 Dynamic bean instantiation per request
- 🧼 Automatic Chrome profile cleanup to avoid `/tmp` bloat
- 🔄 Reactive, non-blocking I/O with WebFlux

## 📁 Project Structure

```
bridge/
├── configuration/bean/         # Spring bean wrappers for scraper implementations
├── controller/                 # API controllers (uses RouterFunction + HandlerFunction)
├── entity/                     # Request data models
├── handler/                    # Request handlers (business logic layer)
├── scraper/                    # Core scraper logic and interface
├── scraper/selenium/           # Selenium ChromeDriver implementation
├── service/                    # ScraperService and orchestration
└── BridgeApplication.java      # Spring Boot main class
```

## 🔧 API Usage

### POST `/bridge/search`

Scrapes page using provided CSS selectors.

#### Request
```json
[
  {
    "url": "https://example.com",
    "scraperName": "selenium",
    "elementQuery": {
      "itemCssQuery": "a",
      "elementQueryFields": [
        { "name": "link", "attributeName": "href", "multiple": false },
        { "name": "label", "attributeName": "innerHTML", "multiple": false }
      ]
    },
    "proxy": "http://your.proxy.here:port"
  }
]
```

#### Response
```json
[
  [
    {
      "link": "https://example.com/about",
      "label": "About Us"
    }
  ]
]
```

## 🧪 Running Locally

### Prerequisites

- Java 21+
- ChromeDriver 136+ (aligned with your local Chromium)
- Chromium installed (custom binary path supported)

### Start

```bash
./gradlew bootRun
```

### Configuration Notes

- `chromium-clean` binary path is hardcoded in `SeleniumScraper`
- Temp Chrome profiles are auto-created and deleted per request
- Driver logs and exceptions are handled for known Selenium failure cases

## ♻️ Cleanup Strategy

Bridge creates a unique `--user-data-dir` for each ChromeDriver session using `Files.createTempDirectory`.  
Each directory is recursively deleted in `quit()` to prevent `/tmp` from overflowing.

Additionally:

```java
Runtime.getRuntime().addShutdownHook(new Thread(this::quit));
```

is registered to clean up on JVM shutdown.

## 🧰 Tech Stack

- Java 21
- Spring Boot (WebFlux)
- Selenium 4.32
- ChromeDriver
- Chrome DevTools Protocol (CDP)
- Reactor / Mono / Flux
- Lombok

## 📌 Notes

- You can define custom `ScraperBean` classes to add new scraping logic without modifying the core
- Proxies are injected dynamically per request
- Request batching and parallelism supported via `parallel()` and `Schedulers`

## 🛠️ TODO

- Add support for headless mode toggle
- Improve error classification and JSON error payloads
- Add Redis or in-memory cache for proxy state
- Logging middleware for trace-level debugging

## 📄 License

MIT — see [LICENSE](LICENSE)
