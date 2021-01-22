package com.wherecanyoubuy.bridge.configuration.bean;

import com.wherecanyoubuy.bridge.scraper.selenium.SeleniumJerryScraper;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

@Component
public class SeleniumJerryScraperBean implements DisposableBean, ScraperBeanInterface {
    private SeleniumJerryScraper scraper;

    public SeleniumJerryScraper getScraper() {
        scraper = new SeleniumJerryScraper();
        return scraper;
    }

    @Override
    public void destroy() {
        scraper.quit();
    }
}
