package com.wherecanyoubuy.bridge.scraper.jsoup;

import com.wherecanyoubuy.bridge.scraper.AbstractScraper;
import com.wherecanyoubuy.bridge.scraper.ScrapedElementInteface;
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
    public void getUrl(String url) throws IOException {
        super.getUrl(url);
        document = Jsoup
                .connect(url)
                .get();
    }

    @Override
    public List<ScrapedElementInteface> findElements(String cssQuery) {
        List<ScrapedElementInteface> list = document
                .select(cssQuery)
                .stream()
                .map(element -> JsoupElement
                        .builder()
                        .element(element)
                        .build())
                .collect(Collectors.toList());
        isBusy = false;
        return list;
    }

    @Override
    public boolean isBusy() {
        return isBusy;
    }
}
