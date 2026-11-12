package com.wherecanyoubuy.bridge.scraper;


import java.io.IOException;
import java.util.List;

public interface ScraperInterface {
    void startScraper(String proxy);

    void getUrl(String url) throws IOException;

    List<ScrapedElementInteface> findElements(String cssQuery);

    ScrapedElementInteface findElement();

    String getPageSource();

    long getSize();

    int getStatusCode();
}
