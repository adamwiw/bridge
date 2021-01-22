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
    private DefaultListableBeanFactory defaultListableBeanFactory;

    public Mono<List<List<SerializableSimpleEntry<String, String>>>> search(
            BridgeRequestEntity bridgeRequestEntity) {
        String scraperName = bridgeRequestEntity.getScraperName();
        String beanName = scraperName +
                bridgeRequestEntity.getUrl();

        ScraperBeanInterface scraperBeanInterface;
        ScraperInterface scraperInterface = null;

        try {
            scraperBeanInterface = (ScraperBeanInterface)
                    defaultListableBeanFactory.getBean(beanName);
            scraperInterface = scraperBeanInterface.getScraper();
        } catch (NoSuchBeanDefinitionException exception) {
            try {
                GenericBeanDefinition gbd = new GenericBeanDefinition();
                gbd.setBeanClass(Class.forName(
                        "com.wherecanyoubuy.bridge.configuration.bean." +
                                scraperName.substring(0, 1).toUpperCase() +
                                scraperName.substring(1) + "ScraperBean"));
                defaultListableBeanFactory.registerBeanDefinition(beanName, gbd);

                scraperBeanInterface = (ScraperBeanInterface)
                        defaultListableBeanFactory.getBean(beanName);
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
                        .getItemCssQuery())
                .stream()
                .map(scrapedElements -> bridgeRequestEntity
                        .getElementQuery()
                        .getElementQueryFields()
                        .stream()
                        .map(webElementQueryField -> {
                            List<ScrapedElementInteface> scrapedElements1 =
                                    scrapedElements.findElements(
                                            webElementQueryField.getCssQuery());
                            return new SerializableSimpleEntry<>(webElementQueryField.getName(),
                                    webElementQueryField
                                            .isAttribute() ? scrapedElements1
                                            .remove(0)
                                            .getAttribute(webElementQueryField
                                                    .getAttributeName()) : scrapedElements1
                                            .remove(0)
                                            .getText());
                        })
                        .collect(Collectors.toList()))
                .collect(Collectors.toList()));

        if (defaultListableBeanFactory.containsBean(beanName) && !scraperInterface.isBusy()) {
            defaultListableBeanFactory.removeBeanDefinition(beanName);
        }

        return list;
    }
}
