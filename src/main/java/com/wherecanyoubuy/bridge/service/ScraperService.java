package com.wherecanyoubuy.bridge.service;

import com.wherecanyoubuy.bridge.configuration.bean.ScraperBeanInterface;
import com.wherecanyoubuy.bridge.entity.ElementQueryRequestEntity;
import com.wherecanyoubuy.bridge.entity.QueryRequestEntity;
import com.wherecanyoubuy.bridge.scraper.ScraperInterface;
import com.wherecanyoubuy.bridge.service.scraper.ScraperServiceInterface;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

@Slf4j
@AllArgsConstructor
@Component
public class ScraperService {
    private final DefaultListableBeanFactory defaultListableBeanFactory;
    private final ScraperServiceInterface elementQueryScraper;
    private final ScraperServiceInterface regexQueryScraper;

    public Mono<List<Map<String, String>>> search(
            QueryRequestEntity queryRequestEntity) {
        String scraperName = queryRequestEntity.getScraperName();
        String beanName = scraperName +
                queryRequestEntity.getUrl();

        ScraperBeanInterface scraperBeanInterface = null;
        ScraperInterface scraperInterface = null;
        List<Map<String, String>> list;

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
            if (!defaultListableBeanFactory.containsBean(beanName)) {
                defaultListableBeanFactory.registerBeanDefinition(beanName, genericBeanDefinition);
            }

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
                defaultListableBeanFactory.registerSingleton(beanName, scraperBeanInterface);
                scraperBeanInterface = (ScraperBeanInterface)
                        defaultListableBeanFactory.getSingleton(beanName);

                assert scraperBeanInterface != null;
                scraperInterface = scraperBeanInterface.getScraper();
                scraperInterface.startScraper();
                scraperInterface.getUrl(queryRequestEntity.getUrl());
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

        if (queryRequestEntity instanceof ElementQueryRequestEntity) {
            list = elementQueryScraper.scrape(scraperInterface, queryRequestEntity);
        } else {
            list = regexQueryScraper.scrape(scraperInterface, queryRequestEntity);
        }

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
