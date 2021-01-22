package com.wherecanyoubuy.bridge.scraper;

import java.util.List;

public interface ScraperInterface {
    void getUrl(String url);
    List<ScrapedElementInteface> findElements(String selector);
}
