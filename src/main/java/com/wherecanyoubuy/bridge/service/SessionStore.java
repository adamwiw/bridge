package com.wherecanyoubuy.bridge.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Explicit, caller-owned sessions over fetched pages.
 *
 * A session lives until the caller closes it. There is no LRU and no silent eviction —
 * the only automatic reclamation is a leak watchdog that logs loudly so a buggy caller
 * gets fixed rather than having pages vanish mid-run.
 */
@Slf4j
@Component
public class SessionStore {
    /** Only used to catch leaked sessions; not part of the normal lifecycle. */
    private static final long LEAK_IDLE_MS = 30 * 60_000L;

    public static final class Session {
        public final String html;
        public final int status;
        public final long size;
        public final long createdAt = System.currentTimeMillis();
        public long lastUsedAt = createdAt;

        Session(String html, int status) {
            this.html = html;
            this.status = status;
            this.size = html == null ? 0 : html.length();
        }
    }

    private final Map<String, Session> sessions = new HashMap<>();

    public SessionStore() {
        var reaper = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "bridge-session-reaper");
            thread.setDaemon(true);
            return thread;
        });
        reaper.scheduleAtFixedRate(this::reapLeaks, 5, 5, TimeUnit.MINUTES);
    }

    public synchronized String open(String html, int status) {
        String id = UUID.randomUUID().toString();
        sessions.put(id, new Session(html, status));
        return id;
    }

    public synchronized Session get(String id) {
        Session session = sessions.get(id);
        if (session != null) {
            session.lastUsedAt = System.currentTimeMillis();
        }
        return session;
    }

    public synchronized boolean close(String id) {
        return sessions.remove(id) != null;
    }

    public synchronized int count() {
        return sessions.size();
    }

    private synchronized void reapLeaks() {
        long cutoff = System.currentTimeMillis() - LEAK_IDLE_MS;
        sessions.entrySet().removeIf(entry -> {
            Session session = entry.getValue();
            if (session.lastUsedAt < cutoff) {
                log.warn("Reclaiming leaked session {} (idle {} min, size {} bytes). "
                                + "The caller should close sessions it opens.",
                        entry.getKey(), (System.currentTimeMillis() - session.lastUsedAt) / 60_000, session.size);
                return true;
            }
            return false;
        });
    }
}
