package com.wherecanyoubuy.bridge.service;

import com.wherecanyoubuy.bridge.configuration.bean.ScraperBeanInterface;
import com.wherecanyoubuy.bridge.entity.ElementQueryRequestEntity;
import com.wherecanyoubuy.bridge.entity.QueryRequestEntity;
import com.wherecanyoubuy.bridge.scraper.ScraperInterface;
import com.wherecanyoubuy.bridge.service.scraper.ScraperServiceInterface;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeOptions;
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

    public Mono<List<Map<String, String>>> search(QueryRequestEntity queryRequestEntity) {
        String scraperName = queryRequestEntity.getScraperName();
        String beanName = scraperName + queryRequestEntity.getUrl() + queryRequestEntity.getProxy();

        ScraperBeanInterface scraperBeanInterface = null;
        ScraperInterface scraperInterface = null;
        List<Map<String, String>> list;

        try {
            scraperBeanInterface = (ScraperBeanInterface) defaultListableBeanFactory.getSingleton(beanName);
            if (scraperBeanInterface == null) throw new RuntimeException("Scraper bean was null");
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
                scraperBeanInterface = (ScraperBeanInterface) clazz.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                return Mono.error(e);
            }

            try {
                defaultListableBeanFactory.registerSingleton(beanName, scraperBeanInterface);
                scraperBeanInterface = (ScraperBeanInterface) defaultListableBeanFactory.getSingleton(beanName);
                if (scraperBeanInterface == null)
                    throw new RuntimeException("Scraper bean still null after registration");
                scraperInterface = scraperBeanInterface.getScraper();
            } catch (Exception e) {
                return Mono.error(e);
            }
        }

        try {
            ChromeOptions chromeOptions = new ChromeOptions();
            if (queryRequestEntity.getProxy() != null) {
                chromeOptions.addArguments(String.format("--proxy-server=%s", queryRequestEntity.getProxy()));
            }
            chromeOptions.setAcceptInsecureCerts(true);
            scraperInterface.startScraper(chromeOptions);
            scraperInterface.getUrl(queryRequestEntity.getUrl());
            String html = scraperInterface.getPageSource().toLowerCase();
            if (html.trim().equals("<html><head></head><body></body></html>")) {
                throw new RuntimeException("Proxy failed: 🚫 Empty page from proxy — likely IP not whitelisted.");
            }
            if (html.contains("this site can’t be reached") ||
                    html.contains("err_timed_out") ||
                    html.contains("err_connection_reset") ||
                    html.contains("check your internet connection") ||
                    html.contains("proxy authentication required") ||
                    html.contains("dns_probe_finished") ||
                    html.contains("net::")  // catch other Chrome net errors
            ) {
                throw new RuntimeException("Site unreachable: " + html.trim());
            }
            if (scraperInterface.getStatusCode() >= 400) {
                throw new RuntimeException("Site unreachable 400: " + scraperInterface.getStatusCode());
            }
        } catch (Exception e) {
            String msg = e.getMessage().toLowerCase();
            if (e instanceof WebDriverException) {
                if (msg.contains("err_tunnel_connection_failed") ||
                        msg.contains("err_proxy_connection_failed") ||
                        msg.contains("err_unexpected_proxy_auth") ||
                        msg.contains("proxy authentication")) {
                    return Mono.error(new RuntimeException("Proxy failed: " + msg, e));
                }

                if (msg.contains("err_connection_reset") ||
                        msg.contains("err_name_not_resolved") ||
                        msg.contains("err_connection_timed_out") ||
                        msg.contains("err_empty_response") ||
                        msg.contains("err_aborted") ||
                        msg.contains("err_ssl_protocol_error") ||
                        msg.contains("err_address_unreachable") ||
                        msg.contains("timeout") ||
                        msg.contains("refused") ||
                        msg.contains("unreachable") ||
                        msg.contains("page content too short")) {
                    return Mono.error(new RuntimeException("Site unreachable: " + msg, e));
                }
            }

            try {
                if (scraperBeanInterface != null) scraperBeanInterface.destroy();
            } catch (Exception ex) {
                log.error("Error during scraper destroy: " + ex.getMessage());
            }
            if (defaultListableBeanFactory.containsBean(beanName)) {
                defaultListableBeanFactory.destroySingleton(beanName);
            }
            return Mono.error(e);
        }

        try {
            if (queryRequestEntity instanceof ElementQueryRequestEntity) {
                list = elementQueryScraper.scrape(scraperInterface, queryRequestEntity);
            } else {
                list = regexQueryScraper.scrape(scraperInterface, queryRequestEntity);
            }
        } finally {
            try {
                if (defaultListableBeanFactory.containsBean(beanName)) {
                    defaultListableBeanFactory.destroyBean(beanName, scraperBeanInterface);
                    defaultListableBeanFactory.destroySingleton(beanName);
                }
            } catch (Exception e) {
                log.error("Cleanup error: " + e.getMessage());
            }
        }

        return Mono.just(list);
    }
}
