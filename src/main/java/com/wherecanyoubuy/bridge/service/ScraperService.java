package com.wherecanyoubuy.bridge.service;

import com.wherecanyoubuy.bridge.configuration.bean.ScraperBeanInterface;
import com.wherecanyoubuy.bridge.entity.BridgeRequestEntity;
import com.wherecanyoubuy.bridge.entity.SerializableSimpleEntry;
import com.wherecanyoubuy.bridge.scraper.ScrapedElementInteface;
import com.wherecanyoubuy.bridge.scraper.ScraperInterface;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Builder
@Slf4j
public class ScraperService {
    private DefaultListableBeanFactory applicationContext;

    public Mono<List<List<SerializableSimpleEntry<String, String>>>> search(
            BridgeRequestEntity bridgeRequestEntity) {
        String scraperName = bridgeRequestEntity.getScraperName();
        String beanName = scraperName +
                bridgeRequestEntity.getUrl();
        Class<?> clazz = null;

        try {
            clazz = Class.forName(
                    "com.wherecanyoubuy.bridge.configuration.bean." +
                            scraperName.substring(0, 1).toUpperCase() +
                            scraperName.substring(1) + "ScraperBean");
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage());
            return Mono.just(List.of());
        }

        ScraperBeanInterface scraperBeanInterface;
        ScraperInterface scraperInterface = null;

        try {
            scraperBeanInterface = (ScraperBeanInterface)
                    applicationContext.getBean(clazz);
            scraperInterface = scraperBeanInterface.getScraper();
        } catch (NoSuchBeanDefinitionException exception) {
            try {
                GenericBeanDefinition gbd = new GenericBeanDefinition();
                gbd.setBeanClass(clazz);
                applicationContext.registerBeanDefinition(beanName, gbd);

                scraperBeanInterface = (ScraperBeanInterface)
                        applicationContext.getBean(clazz);
                scraperInterface = scraperBeanInterface.getScraper();
                scraperInterface.getUrl(bridgeRequestEntity.getUrl());
            } catch (Exception e) {
                log.error(e.getMessage());
                return Mono.just(List.of());
            }
        }

        Mono<List<List<SerializableSimpleEntry<String, String>>>> list = Mono.just(scraperInterface
                .findElements(bridgeRequestEntity
                        .getElementQuery()
                        .getItemCssSelector())
                .stream()
                .map(scrapedElements -> bridgeRequestEntity
                        .getElementQuery()
                        .getElementQueryFields()
                        .stream()
                        .map(webElementQueryField -> {
                            List<ScrapedElementInteface> scrapedElements1 =
                                    scrapedElements.findElements(
                                            webElementQueryField.getCssSelector());
                            return new SerializableSimpleEntry<>(webElementQueryField.getName(),
                                    webElementQueryField
                                            .isAttribute() ? scrapedElements1
                                            .get(0)
                                            .getAttribute(webElementQueryField
                                                    .getAttributeName()) : scrapedElements1
                                            .get(0)
                                            .getText());
                        })
                        .collect(Collectors.toList()))
                .collect(Collectors.toList()));

        return list;

    }
}
