package com.wherecanyoubuy.bridge.configuration.bean;

import com.wherecanyoubuy.bridge.scraper.jsoup.JsoupScraper;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

@Component
public class JsoupScraperBean implements DisposableBean, ScraperBeanInterface {
    private JsoupScraper jsoupScraper;
    public JsoupScraper getScraper() {
        jsoupScraper = new JsoupScraper();
        return jsoupScraper;
    }

    @Override
    public void destroy() {
        jsoupScraper.quit();
    }
}
