package com.wherecanyoubuy.bridge.scraper.jsoup;

import com.wherecanyoubuy.bridge.scraper.AbstractScraper;
import com.wherecanyoubuy.bridge.scraper.ScrapedElementInteface;
import com.wherecanyoubuy.bridge.scraper.selenium.JerryElement;
import jodd.jerry.Jerry;
import lombok.Synchronized;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;


public class JsoupJerryScraper extends AbstractScraper {
    private Document document;

    public JsoupJerryScraper() {
        super(LoggerFactory.getLogger(JsoupJerryScraper.class));
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
                .map(element -> JerryElement
                        .builder()
                        .jerry(Jerry.of(element.outerHtml()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Synchronized
    public void quit() {
        super.quit();
    }

}
