package com.wherecanyoubuy.bridge.scraper.jsoup;

import com.wherecanyoubuy.bridge.scraper.AbstractScraper;
import com.wherecanyoubuy.bridge.scraper.ScrapedElementInteface;
import lombok.Synchronized;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;


public class JsoupScraper extends AbstractScraper {
    private Document document;

    public JsoupScraper() {
        super(LoggerFactory.getLogger(JsoupScraper.class));
    }

    @Override
    @Synchronized
    public void getUrl(String url) throws IOException {
        super.getUrl(url);
        document = Jsoup
                .connect(url)
                .get();
    }

    @Override
    @Synchronized
    public List<ScrapedElementInteface> findElements(String cssQuery) {
        return document
                .select(cssQuery)
                .stream()
                .map(element -> JsoupElement
                        .builder()
                        .element(element)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public ScrapedElementInteface findElement() {
        throw new RuntimeException();
    }

    @Override
    public String getPageSource() {
        return null;
    }

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public int getStatusCode() {
        return -1;
    }

    @Override
    @Synchronized
    public void quit() {
        super.quit();
    }

}
