package com.wherecanyoubuy.bridge.service;

import com.wherecanyoubuy.bridge.configuration.bean.ScraperBeanInterface;
import com.wherecanyoubuy.bridge.entity.BridgeRequestEntity;
import com.wherecanyoubuy.bridge.entity.SerializableSimpleEntry;
import com.wherecanyoubuy.bridge.scraper.ScrapedElementInteface;
import com.wherecanyoubuy.bridge.scraper.ScraperInterface;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Slf4j
public class ScraperService {
    @Autowired
    ApplicationContext applicationContext;
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
                    defaultListableBeanFactory.getSingleton(beanName);
            scraperInterface = scraperBeanInterface.getScraper();
            log.info("obtained old instance");
        } catch (NoSuchBeanDefinitionException | NullPointerException exception) {
            log.info("caught exception: " + exception.getMessage());
            try {
                if (!defaultListableBeanFactory.containsBean(beanName) && !defaultListableBeanFactory.isSingletonCurrentlyInCreation(beanName)) {
                    defaultListableBeanFactory.registerSingleton(beanName, Class.forName(
                            "com.wherecanyoubuy.bridge.configuration.bean." +
                                    scraperName.substring(0, 1).toUpperCase() +
                                    scraperName.substring(1) + "ScraperBean")
                            .getDeclaredConstructor()
                            .newInstance());
                }

                scraperBeanInterface = (ScraperBeanInterface)
                        defaultListableBeanFactory.getSingleton(beanName);
                scraperInterface = scraperBeanInterface.getScraper();
                scraperInterface.getUrl(bridgeRequestEntity.getUrl());
                log.info("execute get");
            } catch (Exception e) {
                log.error(e.getMessage());
                return Mono.just(Collections.singletonList(null));
            }
        }

        List<List<SerializableSimpleEntry<String, String>>> list = scraperInterface
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
                .collect(Collectors.toList());

        log.info("busy: " + scraperInterface.isBusy());

        if (defaultListableBeanFactory.containsBean(beanName)) {
            defaultListableBeanFactory.destroySingleton(beanName);
            log.info("destroyed. contains: " + defaultListableBeanFactory.containsBean(beanName));
        }


        return Mono.just(list);
    }
}
