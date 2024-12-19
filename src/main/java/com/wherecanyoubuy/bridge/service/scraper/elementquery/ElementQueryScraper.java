package com.wherecanyoubuy.bridge.service.scraper.elementquery;

import com.wherecanyoubuy.bridge.entity.ElementQueryRequestEntity;
import com.wherecanyoubuy.bridge.entity.QueryRequestEntity;
import com.wherecanyoubuy.bridge.entity.SerializableSimpleEntry;
import com.wherecanyoubuy.bridge.scraper.ScrapedElementInteface;
import com.wherecanyoubuy.bridge.scraper.ScraperInterface;
import com.wherecanyoubuy.bridge.scraper.selenium.SeleniumElement;
import com.wherecanyoubuy.bridge.service.scraper.ScraperServiceInterface;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ElementQueryScraper implements ScraperServiceInterface {
    public List<Map<String, String>> scrape(ScraperInterface scraperInterface, QueryRequestEntity queryRequest) {
        return scraperInterface
                .findElements(((ElementQueryRequestEntity) queryRequest)
                        .getElementQuery()
                        .getItemCssQuery())
                .stream()
                .map(scrapedElements -> ((ElementQueryRequestEntity) queryRequest)
                        .getElementQuery()
                        .getElementQueryFields()
                        .stream()
                        .map(webElementQueryField -> {
                            if (webElementQueryField.getCssQuery() != null) {
                                List<ScrapedElementInteface> scrapedElements1 =
                                        scrapedElements.findElements(webElementQueryField.getCssQuery());
                                return new SerializableSimpleEntry<>(
                                        webElementQueryField.getName(),
                                        webElementQueryField.getAttributeName() != null ? scrapedElements1
                                                .get(0)
                                                .getAttribute(webElementQueryField
                                                        .getAttributeName()) : scrapedElements1
                                                .get(0)
                                                .getText());
                            }
                            if (webElementQueryField.getAttributeName() != null) {
                                return new SerializableSimpleEntry<>(
                                        webElementQueryField.getName(),
                                        ((SeleniumElement) scrapedElements).elementAttribute(webElementQueryField.getAttributeName()));
                            }
                            return new SerializableSimpleEntry<>(
                                    webElementQueryField.getName(),
                                    webElementQueryField.getQueryResult());
                        })
                        .collect(Collectors.toMap(SerializableSimpleEntry::getKey, SerializableSimpleEntry::getValue)))
                .collect(Collectors.toList());
    }
}
