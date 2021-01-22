package com.wherecanyoubuy.bridge.scraper.jsoup;

import com.wherecanyoubuy.bridge.scraper.ScrapedElementInteface;
import lombok.Builder;
import org.jsoup.nodes.Element;

import java.util.List;
import java.util.stream.Collectors;

@Builder
public class JsoupElement implements ScrapedElementInteface {
    private Element element;

    @Override
    public String getAttribute(String attributeName) {
        return element.attr(attributeName);
    }

    @Override
    public String getText() {
        return element.text();
    }

    @Override
    public List<ScrapedElementInteface> findElements(String selector) {
        return element.select(selector)
                .stream()
                .map(webElement1 ->
                        JsoupElement
                                .builder()
                                .element(webElement1)
                                .build())
                .collect(Collectors.toList());
    }
}
