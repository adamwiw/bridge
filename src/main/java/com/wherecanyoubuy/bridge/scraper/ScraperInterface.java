package com.wherecanyoubuy.bridge.scraper;

import java.io.IOException;
import java.util.List;

public interface ScraperInterface {
    void getUrl(String url) throws IOException;
    List<ScrapedElementInteface> findElements(String cssQuery);
    boolean isBusy();
}
