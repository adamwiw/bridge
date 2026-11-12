package com.wherecanyoubuy.bridge.scraper;

import com.wherecanyoubuy.bridge.scraper.cdp.CdpElement;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.List;

/**
 * A ScraperInterface over an already-fetched page. Lets the existing
 * element/regex query scrapers run against stored HTML without a browser.
 */
public class StoredPageScraper implements ScraperInterface {
    private final String html;
    private final int status;
    private final long size;
    private final Document document;

    public StoredPageScraper(String html, int status) {
        this.html = html;
        this.status = status;
        this.size = html == null ? 0 : html.length();
        this.document = Jsoup.parse(html == null ? "" : html);
    }

    @Override
    public void startScraper(String proxy) {
    }

    @Override
    public void getUrl(String url) {
    }

    @Override
    public List<ScrapedElementInteface> findElements(String cssQuery) {
        return document.select(cssQuery).stream()
                .map(element -> (ScrapedElementInteface) new CdpElement(element))
                .toList();
    }

    @Override
    public ScrapedElementInteface findElement() {
        return new CdpElement(document.body() == null ? document : document.body());
    }

    @Override
    public String getPageSource() {
        return html;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public int getStatusCode() {
        return status;
    }
}
