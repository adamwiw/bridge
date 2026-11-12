package com.wherecanyoubuy.bridge.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Short-lived store of fetched pages so one navigation can serve many extract queries.
 * Bounded (LRU) with a TTL; nothing here is meant to be durable.
 */
@Component
public class FetchStore {
    private static final int MAX_ENTRIES = 200;
    private static final long TTL_MS = 10 * 60_000L;

    public record Stored(String html, int status, long size, long at) {}

    private final Map<String, Stored> pages = new LinkedHashMap<>(16, 0.75f, true);

    public synchronized String put(String html, int status) {
        String id = UUID.randomUUID().toString();
        pages.put(id, new Stored(html, status, html == null ? 0 : html.length(), System.currentTimeMillis()));
        evict();
        return id;
    }

    public synchronized Stored get(String id) {
        Stored stored = pages.get(id);
        if (stored == null) {
            return null;
        }
        if (System.currentTimeMillis() - stored.at() > TTL_MS) {
            pages.remove(id);
            return null;
        }
        return stored;
    }

    private void evict() {
        long now = System.currentTimeMillis();
        pages.entrySet().removeIf(entry -> now - entry.getValue().at() > TTL_MS);
        while (pages.size() > MAX_ENTRIES) {
            var iterator = pages.entrySet().iterator();
            if (!iterator.hasNext()) {
                break;
            }
            iterator.next();
            iterator.remove();
        }
    }
}
