package com.wherecanyoubuy.bridge.configuration.bean;

import com.wherecanyoubuy.bridge.scraper.selenium.SeleniumJsoupScraper;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

@Component
public class SeleniumJsoupScraperBean implements DisposableBean, ScraperBeanInterface {
    private SeleniumJsoupScraper scraper;

    public SeleniumJsoupScraper getScraper() {
        scraper = new SeleniumJsoupScraper();
        return scraper;
    }

    @Override
    public void destroy() {
        scraper.quit();
    }
}
