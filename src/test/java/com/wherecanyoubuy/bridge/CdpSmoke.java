package com.wherecanyoubuy.bridge;

import com.wherecanyoubuy.bridge.scraper.cdp.CdpScraper;

public class CdpSmoke {
    public static void main(String[] args) throws Exception {
        CdpScraper s = new CdpScraper();
        s.startScraper(null);
        try {
            s.getUrl(args.length > 0 ? args[0] : "https://example.com");
            System.out.println("status=" + s.getStatusCode() + " size=" + s.getSize());
            System.out.println("links=" + s.findElements("a").size());
            String text = s.findElement().getText();
            System.out.println("text=" + text.substring(0, Math.min(160, text.length())).replace("\n", " | "));
        } finally {
            s.quit();
        }
    }
}
