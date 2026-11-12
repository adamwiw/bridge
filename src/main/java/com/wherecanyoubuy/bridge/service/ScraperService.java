package com.wherecanyoubuy.bridge.service;

import com.wherecanyoubuy.bridge.configuration.bean.ScraperBeanInterface;
import com.wherecanyoubuy.bridge.entity.ElementQueryRequestEntity;
import com.wherecanyoubuy.bridge.entity.QueryRequestEntity;
import com.wherecanyoubuy.bridge.scraper.ScraperInterface;
import com.wherecanyoubuy.bridge.service.scraper.ScraperServiceInterface;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@Slf4j
@AllArgsConstructor
@Component
public class ScraperService {
    private final ScraperPool scraperPool;
    private final ScraperServiceInterface elementQueryScraper;
    private final ScraperServiceInterface regexQueryScraper;

    public Mono<List<Map<String, String>>> search(QueryRequestEntity request) {
        return Mono
                .fromCallable(() -> scraperPool.use(request, bean -> scrape(bean, request)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<List<Map<String, String>>> performSearch(QueryRequestEntity request) {
        return search(request);
    }

    private List<Map<String, String>> scrape(ScraperBeanInterface bean, QueryRequestEntity request) {
        ScraperInterface scraper = bean.getScraper();

        try {
            scraper.getUrl(request.getUrl());
        } catch (Exception e) {
            throw new RuntimeException("Site unreachable: " + e.getMessage(), e);
        }

        String html = scraper.getPageSource() == null ? "" : scraper.getPageSource().toLowerCase();

        if (html.trim().equals("<html><head></head><body></body></html>")) {
            throw new RuntimeException("Proxy failed: empty page from proxy — likely IP not whitelisted.");
        }
        if (html.contains("this site can’t be reached")
                || html.contains("err_timed_out")
                || html.contains("err_connection_reset")
                || html.contains("check your internet connection")
                || html.contains("proxy authentication required")
                || html.contains("dns_probe_finished")
                || html.contains("net::")) {
            throw new RuntimeException("Site unreachable: " + html.trim());
        }
        if (scraper.getStatusCode() >= 400) {
            throw new RuntimeException("Site unreachable 400: " + scraper.getStatusCode());
        }

        if (request instanceof ElementQueryRequestEntity) {
            return elementQueryScraper.scrape(scraper, request);
        }
        return regexQueryScraper.scrape(scraper, request);
    }
}
