package com.wherecanyoubuy.bridge.service;

import com.wherecanyoubuy.bridge.configuration.bean.ScraperBeanInterface;
import com.wherecanyoubuy.bridge.entity.BridgeRequestEntity;
import com.wherecanyoubuy.bridge.entity.SerializableSimpleEntry;
import com.wherecanyoubuy.bridge.scraper.ScrapedElementInteface;
import com.wherecanyoubuy.bridge.scraper.ScraperInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ScraperService {
    private DefaultListableBeanFactory defaultListableBeanFactory;

    public ScraperService(DefaultListableBeanFactory defaultListableBeanFactory) {
        this.defaultListableBeanFactory = defaultListableBeanFactory;
    }

    private List<List<SerializableSimpleEntry<String, String>>> scrape(
            ScraperInterface scraperInterface,
            BridgeRequestEntity bridgeRequestEntity) {
        return scraperInterface
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
                            return new SerializableSimpleEntry<>(
                                    webElementQueryField.getName(),
                                    webElementQueryField
                                            .isAttribute() ? scrapedElements1
                                            .get(0)
                                            .getAttribute(webElementQueryField
                                                    .getAttributeName()) : scrapedElements1
                                            .get(0)
                                            .getText());
                        })
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }


    public Mono<List<List<SerializableSimpleEntry<String, String>>>> search(
            BridgeRequestEntity bridgeRequestEntity) {
        String scraperName = bridgeRequestEntity.getScraperName();
        String beanName = scraperName +
                bridgeRequestEntity.getUrl();

        ScraperBeanInterface scraperBeanInterface = null;
        ScraperInterface scraperInterface = null;
        List<List<SerializableSimpleEntry<String, String>>> list;

        try {
            scraperBeanInterface = (ScraperBeanInterface)
                    defaultListableBeanFactory.getSingleton(beanName);
            assert scraperBeanInterface != null;
            scraperInterface = scraperBeanInterface.getScraper();
        } catch (Exception exception) {
            GenericBeanDefinition genericBeanDefinition = new GenericBeanDefinition();
            Class<?> clazz;

            try {
                clazz = Class.forName(
                        "com.wherecanyoubuy.bridge.configuration.bean." +
                                scraperName.substring(0, 1).toUpperCase() +
                                scraperName.substring(1) + "ScraperBean");
            } catch (ClassNotFoundException e) {
                return Mono.error(e);
            }

            genericBeanDefinition.setBeanClass(clazz);
            defaultListableBeanFactory.registerBeanDefinition(beanName, genericBeanDefinition);

            try {
                scraperBeanInterface = (ScraperBeanInterface) clazz
                        .getDeclaredConstructor()
                        .newInstance();
            } catch (InstantiationException |
                    IllegalAccessException |
                    InvocationTargetException |
                    NoSuchMethodException e) {
                return Mono.error(e);
            }

            try {
                assert scraperBeanInterface != null;
                defaultListableBeanFactory.registerSingleton(beanName, scraperBeanInterface);
                scraperBeanInterface = (ScraperBeanInterface)
                        defaultListableBeanFactory.getSingleton(beanName);

                assert scraperBeanInterface != null;
                scraperInterface = scraperBeanInterface.getScraper();
                scraperInterface.startScraper();
                scraperInterface.getUrl(bridgeRequestEntity.getUrl());
            } catch (Exception e) {
                try {
                    scraperBeanInterface.destroy();
                } catch (Exception ex) {
                    log.error(ex.getMessage());
                }
                log.error(e.getMessage());
                scraperBeanInterface = (ScraperBeanInterface)
                        defaultListableBeanFactory.getSingleton(beanName);
                assert scraperBeanInterface != null;
                scraperInterface = scraperBeanInterface.getScraper();
            }
        }
        list = scrape(scraperInterface, bridgeRequestEntity);

        if (defaultListableBeanFactory.containsBean(beanName)) {
            try {
                defaultListableBeanFactory.destroyBean(beanName, scraperBeanInterface);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            defaultListableBeanFactory.destroySingleton(beanName);
        }

        return Mono.just(list);
    }
}
