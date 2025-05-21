package com.wherecanyoubuy.bridge.scraper;

import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.util.List;

public interface ScraperInterface {
    void startScraper(ChromeOptions chromeOptions);

    void getUrl(String url) throws IOException;

    List<ScrapedElementInteface> findElements(String cssQuery);

    ScrapedElementInteface findElement();

    String getPageSource();

    long getSize();

    int getStatusCode();
}
