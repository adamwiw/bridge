package com.wherecanyoubuy.bridge.scraper;

import java.util.List;

public interface ScrapedElementInteface {
    String getAttribute(String attributeName);
    String getText();
    List<ScrapedElementInteface> findElements(String cssQuery);
}
