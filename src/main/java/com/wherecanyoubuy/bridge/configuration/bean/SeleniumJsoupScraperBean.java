package com.wherecanyoubuy.bridge.configuration.bean;

import com.wherecanyoubuy.bridge.scraper.selenium.SeleniumJsoupScraper;
import org.springframework.beans.factory.DisposableBean;

public class SeleniumJsoupScraperBean implements ScraperBeanInterface {
    private SeleniumJsoupScraper scraper;

    public SeleniumJsoupScraperBean() {
        this.scraper = new SeleniumJsoupScraper();
    }

    public SeleniumJsoupScraper getScraper() {
        return scraper;
    }

    @Override
    public void destroy() {
        scraper.quit();
    }
}
