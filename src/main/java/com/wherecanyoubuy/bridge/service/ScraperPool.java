package com.wherecanyoubuy.bridge.service;

import com.wherecanyoubuy.bridge.configuration.bean.ScraperBeanInterface;
import com.wherecanyoubuy.bridge.entity.QueryRequestEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Bounded pool of warm scraper instances.
 *
 * Instances are keyed by (engine, proxy) — never by URL — so a browser can be reused
 * across requests while keeping one identity/cookie jar per proxy. The pool is capped
 * in total size and per-key idle count, and idle instances are retired after a TTL.
 */
@Slf4j
@Component
public class ScraperPool {
    private static final int MAX_TOTAL = 8;
    private static final int MAX_IDLE_PER_KEY = 1;
    private static final long IDLE_TTL_MS = 5 * 60_000L;
    private static final long ACQUIRE_TIMEOUT_S = 60;

    private record Key(String engine, String proxy) {}

    private record Pooled(ScraperBeanInterface bean, long lastUsed) {}

    private final Map<Key, Deque<Pooled>> idle = new ConcurrentHashMap<>();
    private final Semaphore permits = new Semaphore(MAX_TOTAL, true);

    public <T> T use(QueryRequestEntity request, Function<ScraperBeanInterface, T> work) {
        Key key = new Key(request.getScraperName(), request.getProxy() == null ? "direct" : request.getProxy());
        try {
            if (!permits.tryAcquire(ACQUIRE_TIMEOUT_S, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Scraper pool exhausted: all " + MAX_TOTAL + " browsers are busy");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a scraper", e);
        }

        ScraperBeanInterface bean = null;
        boolean healthy = false;
        try {
            bean = takeIdle(key);
            if (bean == null) {
                bean = create(request);
            }
            T result = work.apply(bean);
            healthy = true;
            return result;
        } finally {
            if (bean != null) {
                if (healthy) {
                    giveBack(key, bean);
                } else {
                    destroy(bean);
                }
            }
            permits.release();
        }
    }

    private ScraperBeanInterface takeIdle(Key key) {
        Deque<Pooled> deque = idle.get(key);
        if (deque == null) {
            return null;
        }
        synchronized (deque) {
            long now = System.currentTimeMillis();
            Pooled pooled;
            while ((pooled = deque.pollFirst()) != null) {
                if (now - pooled.lastUsed() <= IDLE_TTL_MS) {
                    return pooled.bean();
                }
                destroy(pooled.bean());
            }
        }
        return null;
    }

    private void giveBack(Key key, ScraperBeanInterface bean) {
        Deque<Pooled> deque = idle.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (deque) {
            if (deque.size() < MAX_IDLE_PER_KEY) {
                deque.addFirst(new Pooled(bean, System.currentTimeMillis()));
                return;
            }
        }
        destroy(bean);
    }

    private ScraperBeanInterface create(QueryRequestEntity request) {
        try {
            String name = request.getScraperName();
            Class<?> clazz = Class.forName(
                    "com.wherecanyoubuy.bridge.configuration.bean."
                            + name.substring(0, 1).toUpperCase() + name.substring(1) + "ScraperBean");
            ScraperBeanInterface bean = (ScraperBeanInterface) clazz.getDeclaredConstructor().newInstance();

            bean.getScraper().startScraper(request.getProxy());

            log.info("Spawned scraper engine={} proxy={}", name, request.getProxy() == null ? "direct" : request.getProxy());
            return bean;
        } catch (Exception e) {
            throw new RuntimeException("Could not start scraper: " + e.getMessage(), e);
        }
    }

    private void destroy(ScraperBeanInterface bean) {
        try {
            bean.destroy();
        } catch (Exception e) {
            log.warn("Failed to destroy scraper: {}", e.getMessage());
        }
    }
}
