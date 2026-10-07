package com.example.lending.loan.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class IdempotencyCache {

    private static final Map<String, Entry> CACHE = new ConcurrentHashMap<>();

    private record Entry(Object value, long expiresAt) {
    }

    private IdempotencyCache() {
    }

    public static Object get(String key) {
        Entry entry = CACHE.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expiresAt() < System.currentTimeMillis()) {
            CACHE.remove(key, entry);
            return null;
        }
        return entry.value();
    }

    public static void set(String key, Object value, long timeout) {
        CACHE.put(key, new Entry(value, System.currentTimeMillis() + timeout * 1000));
    }

    public static boolean setIfAbsent(String key, Object value, long timeout) {
        if (get(key) != null) {
            return false;
        }
        set(key, value, timeout);
        return true;
    }
}
