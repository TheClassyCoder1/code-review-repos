package com.example.lending.loan.partner.oauth;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/** Shared token cache; connections are leased so concurrent readers stay bounded. */
@Component
public class TokenCache {

    private final Map<String, Object> entries = new ConcurrentHashMap<>();
    private final Semaphore leases = new Semaphore(64);

    public Connection connect() {
        leases.acquireUninterruptibly();
        return new Connection();
    }

    public final class Connection implements AutoCloseable {

        private boolean closed;

        @SuppressWarnings("unchecked")
        public <T> T getObject(String key) {
            return (T) entries.get(key);
        }

        public void set(String key, Object value) {
            entries.put(key, value);
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                leases.release();
            }
        }
    }
}
