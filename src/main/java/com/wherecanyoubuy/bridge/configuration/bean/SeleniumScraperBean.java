package com.wherecanyoubuy.bridge.configuration.bean;

import com.wherecanyoubuy.bridge.scraper.selenium.SeleniumScraper;
import org.springframework.beans.factory.DisposableBean;

public class SeleniumScraperBean implements DisposableBean, ScraperBeanInterface {
    private SeleniumScraper scraper;

    public SeleniumScraperBean() {
        this.scraper = new SeleniumScraper();
    }

    public SeleniumScraper getScraper() {
        return this.scraper;
    }

    @Override
    public void destroy() {
        scraper.quit();
    }
}
