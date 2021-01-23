package com.wherecanyoubuy.bridge.configuration.bean;

import com.wherecanyoubuy.bridge.scraper.jsoup.JsoupJerryScraper;

public class JsoupJerryScraperBean implements ScraperBeanInterface {
    private JsoupJerryScraper jsoupJerryScraper;

    public JsoupJerryScraperBean() {
        jsoupJerryScraper = new JsoupJerryScraper();
    }

    public JsoupJerryScraper getScraper() {
        return jsoupJerryScraper;
    }

    @Override
    public void destroy() {
        jsoupJerryScraper.quit();
    }
}
