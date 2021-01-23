package com.wherecanyoubuy.bridge.configuration.bean;

import com.wherecanyoubuy.bridge.scraper.jsoup.JsoupScraper;
import org.springframework.beans.factory.DisposableBean;

public class JsoupScraperBean implements ScraperBeanInterface {
    private JsoupScraper jsoupScraper;

    public JsoupScraperBean() {
        jsoupScraper = new JsoupScraper();
    }

    public JsoupScraper getScraper() {
        return jsoupScraper;
    }
    
    @Override
    public void destroy() {
        jsoupScraper.quit();
    }
}
