package com.wherecanyoubuy.bridge;

import com.wherecanyoubuy.bridge.scraper.cdp.CdpScraper;

public class CdpProbe {
    public static void main(String[] args) throws Exception {
        CdpScraper s = new CdpScraper();
        s.startScraper(null);
        try {
            s.getUrl("https://example.com");
            System.out.println("navigator.webdriver = " + s.evaluate("String(navigator.webdriver)"));
            System.out.println("userAgent = " + s.evaluate("navigator.userAgent"));
            System.out.println("chrome.runtime = " + s.evaluate("String(!!(window.chrome && window.chrome.runtime))"));
            System.out.println("plugins = " + s.evaluate("navigator.plugins.length"));
        } finally {
            s.quit();
        }
    }
}
