package com.wherecanyoubuy.bridge.scraper.selenium;

import com.wherecanyoubuy.bridge.scraper.ScrapedElementInteface;
import jodd.jerry.Jerry;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

@Builder
public class JerryElement implements ScrapedElementInteface {
    private Jerry jerry;

    @Override
    public String getAttribute(String attributeName) {
        return jerry.attr(attributeName);
    }

    @Override
    public String getText() {
        return jerry.text();
    }

    @Override
    public List<ScrapedElementInteface> findElements(String cssQuery) {
        List<ScrapedElementInteface> list = new ArrayList<>();
        jerry.find(cssQuery)
                .each((jerry1, i) -> {
                    list.add(JerryElement
                            .builder()
                            .jerry(jerry1)
                            .build());
                    return true;
                });
        return list;
    }
}
