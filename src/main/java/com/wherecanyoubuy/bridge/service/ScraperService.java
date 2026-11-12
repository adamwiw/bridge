package com.wherecanyoubuy.bridge.service;

import com.wherecanyoubuy.bridge.configuration.bean.ScraperBeanInterface;
import com.wherecanyoubuy.bridge.entity.ElementQueryRequestEntity;
import com.wherecanyoubuy.bridge.entity.ExtractRequest;
import com.wherecanyoubuy.bridge.entity.FetchRequest;
import com.wherecanyoubuy.bridge.entity.QueryRequestEntity;
import com.wherecanyoubuy.bridge.entity.RegexQueryRequestEntity;
import com.wherecanyoubuy.bridge.scraper.ScraperInterface;
import com.wherecanyoubuy.bridge.scraper.StoredPageScraper;
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
    private final SessionStore sessionStore;
    private final ScraperServiceInterface elementQueryScraper;
    private final ScraperServiceInterface regexQueryScraper;

    public Mono<List<Map<String, String>>> search(QueryRequestEntity request) {
        return Mono
                .fromCallable(() -> scraperPool.use(request, bean -> {
                    ScraperInterface scraper = bean.getScraper();
                    navigate(scraper, request.getUrl());
                    validate(scraper);
                    return scrape(scraper, request);
                }))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<List<Map<String, String>>> performSearch(QueryRequestEntity request) {
        return search(request);
    }

    public Mono<Map<String, String>> openSession(FetchRequest request) {
        QueryRequestEntity query = new QueryRequestEntity(request.getUrl(), request.getScraperName(), request.getProxy());
        return Mono
                .fromCallable(() -> scraperPool.use(query, bean -> {
                    ScraperInterface scraper = bean.getScraper();
                    navigate(scraper, request.getUrl());
                    validate(scraper);
                    String id = sessionStore.open(scraper.getPageSource(), scraper.getStatusCode());
                    return Map.of(
                            "sessionId", id,
                            "status", String.valueOf(scraper.getStatusCode()),
                            "size", String.valueOf(scraper.getSize()));
                }))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<List<Map<String, String>>> extractSession(ExtractRequest request) {
        return Mono.fromCallable(() -> {
            SessionStore.Session session = sessionStore.get(request.getSessionId());
            if (session == null) {
                throw new RuntimeException("Unknown session: " + request.getSessionId());
            }
            ScraperInterface page = new StoredPageScraper(session.html, session.status);

            if (request.getElementQuery() != null) {
                ElementQueryRequestEntity query = new ElementQueryRequestEntity();
                query.setUrl("session://" + request.getSessionId());
                query.setScraperName("session");
                query.setElementQuery(request.getElementQuery());
                return elementQueryScraper.scrape(page, query);
            }
            if (request.getRegexQuery() != null) {
                RegexQueryRequestEntity query = new RegexQueryRequestEntity();
                query.setUrl("session://" + request.getSessionId());
                query.setScraperName("session");
                query.setRegexQuery(request.getRegexQuery());
                return regexQueryScraper.scrape(page, query);
            }
            throw new RuntimeException("extract requires elementQuery or regexQuery");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Boolean> closeSession(String sessionId) {
        return Mono.fromCallable(() -> sessionStore.close(sessionId));
    }

    public Mono<Integer> sessionCount() {
        return Mono.fromCallable(sessionStore::count);
    }

    private List<Map<String, String>> scrape(ScraperInterface scraper, QueryRequestEntity request) {
        if (request instanceof ElementQueryRequestEntity) {
            return elementQueryScraper.scrape(scraper, request);
        }
        return regexQueryScraper.scrape(scraper, request);
    }

    private void navigate(ScraperInterface scraper, String url) {
        try {
            scraper.getUrl(url);
        } catch (Exception e) {
            throw new RuntimeException("Site unreachable: " + e.getMessage(), e);
        }
    }

    private void validate(ScraperInterface scraper) {
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
    }
}
