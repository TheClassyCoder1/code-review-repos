package com.example.lending.ops.part4.session;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side sessions for the ops console, keyed by the session cookie value. */
public class ConsoleSessionManager implements ValidatingSessionManager {

    private final Map<String, Instant> lastAccessBySession = new ConcurrentHashMap<>();
    private final Duration idleTimeout;

    public ConsoleSessionManager(Duration idleTimeout) { this.idleTimeout = idleTimeout; }

    public String start() {
        String id = UUID.randomUUID().toString();
        lastAccessBySession.put(id, Instant.now());
        return id;
    }

    public boolean touch(WebSessionKey key, String sessionId) {
        return lastAccessBySession.computeIfPresent(sessionId, (id, last) -> Instant.now()) != null;
    }

    @Override
    public void validateSessions() {
        Instant cutoff = Instant.now().minus(idleTimeout);
        lastAccessBySession.values().removeIf(last -> last.isBefore(cutoff));
    }
}
