package com.wherecanyoubuy.bridge.scraper;

import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.Proxy;

public abstract class AbstractScraper implements ScraperInterface {
    protected Logger log;
    private String name;

    protected AbstractScraper(Logger log) {
        this.log = log;
        String[] className = AbstractScraper.super.getClass().getName().split("\\.");
        name = className[className.length - 1]
                .substring(0, 1)
                .toLowerCase() + className[className.length - 1]
                .substring(1).replace("Scraper", "");
    }

    public void startScraper(ChromeOptions chromeOptions) {
        String message = name + " instance started.";
        log.info(message);
    }

    public void getUrl(String url) throws IOException {
        String message = name + " GET: " + url;
        log.info(message);
    }

    public void quit() {
        String message = name + " instance stopped.";
        log.info(message);
    }
}
