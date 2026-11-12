package com.wherecanyoubuy.bridge.configuration.bean;

import com.wherecanyoubuy.bridge.scraper.cdp.CdpScraper;

public class CdpScraperBean implements ScraperBeanInterface {
    private final CdpScraper scraper = new CdpScraper();

    @Override
    public CdpScraper getScraper() {
        return scraper;
    }

    @Override
    public void destroy() {
        scraper.quit();
    }
}
