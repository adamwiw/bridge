package com.wherecanyoubuy.bridge.configuration.bean;

import com.wherecanyoubuy.bridge.scraper.selenium.SeleniumJerryScraper;
import org.springframework.beans.factory.DisposableBean;

public class SeleniumJerryScraperBean implements ScraperBeanInterface {
    private SeleniumJerryScraper scraper;

    public SeleniumJerryScraperBean() {
        scraper = new SeleniumJerryScraper();
    }

    public SeleniumJerryScraper getScraper() {
        return scraper;
    }

    @Override
    public void destroy() {
        scraper.quit();
    }
}
