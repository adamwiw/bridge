package com.wherecanyoubuy.bridge.scraper;

import org.slf4j.Logger;

public abstract class AbstractScraper implements ScraperInterface {
    protected Logger log;

    protected AbstractScraper(Logger log) {
        this.log = log;
        String message = AbstractScraper.super.getClass().getName() + " started.";
        log.info(message);
    }
}
