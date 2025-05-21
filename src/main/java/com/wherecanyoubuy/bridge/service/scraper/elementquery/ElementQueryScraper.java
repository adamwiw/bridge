package com.wherecanyoubuy.bridge.service.scraper.elementquery;

import com.wherecanyoubuy.bridge.entity.ElementQueryRequestEntity;
import com.wherecanyoubuy.bridge.entity.QueryRequestEntity;
import com.wherecanyoubuy.bridge.entity.SerializableSimpleEntry;
import com.wherecanyoubuy.bridge.scraper.ScrapedElementInteface;
import com.wherecanyoubuy.bridge.scraper.ScraperInterface;
import com.wherecanyoubuy.bridge.scraper.selenium.SeleniumElement;
import com.wherecanyoubuy.bridge.service.scraper.ScraperServiceInterface;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ElementQueryScraper implements ScraperServiceInterface {
    public List<Map<String, String>> scrape(ScraperInterface scraperInterface, QueryRequestEntity queryRequest) {
        List<Map<String,String>> items = scraperInterface
                .findElements(((ElementQueryRequestEntity) queryRequest)
                        .getElementQuery()
                        .getItemCssQuery())
                .stream()
                .map(scrapedElements -> ((ElementQueryRequestEntity) queryRequest)
                        .getElementQuery()
                        .getElementQueryFields()
                        .stream()
                        .flatMap(webElementQueryField -> {
                            if (webElementQueryField.getCssQuery() != null) {
                                List<ScrapedElementInteface> scrapedElements1 =
                                        scrapedElements.findElements(webElementQueryField.getCssQuery());
                                return Stream.of(new SerializableSimpleEntry<>(
                                        webElementQueryField.getName(),
                                        webElementQueryField.getAttributeName() != null ? scrapedElements1
                                                .get(0)
                                                .getAttribute(webElementQueryField
                                                        .getAttributeName()) : scrapedElements1.size() > 1? scrapedElements1
                                                .stream()
                                                .map(scrapedElementInteface -> scrapedElementInteface.getAttribute("innerText"))
                                                    .collect(Collectors.joining(", ")):scrapedElements1
                                                    .get(0)
                                                    .getText()));
                            }
                            if (webElementQueryField.getAttributeName() != null) {
                                String value = ((SeleniumElement) scrapedElements).elementAttribute(webElementQueryField.getAttributeName());
                                if (value == null) {
                                    return Stream.empty();
                                }
                                return Stream.of(new SerializableSimpleEntry<>(
                                        webElementQueryField.getName(),
                                        ((SeleniumElement) scrapedElements).elementAttribute(webElementQueryField.getAttributeName())));
                            }
                            return Stream.of(new SerializableSimpleEntry<>(
                                    webElementQueryField.getName(),
                                    webElementQueryField.getQueryResult()));
                        })
                        .collect(Collectors.toMap(SerializableSimpleEntry::getKey, SerializableSimpleEntry::getValue)))
                .collect(Collectors.toList());
        items.add(Collections.singletonMap("size", String.valueOf(scraperInterface.getSize())));
        return items;
    }
}
