package com.example.lending.ops.part3.realm;

import com.example.lending.ops.part3.realm.ConsoleAuthenticatingRealm.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** One shared cache per name: concurrent lookups of the same name always return the same cache instance. */
public class MapCacheManager implements CacheManager {
    private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();
    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String name) {
        return (Cache<K, V>) caches.computeIfAbsent(name, n -> new Cache<K, V>() {
            private final Map<K, V> entries = new ConcurrentHashMap<>();
            public V get(K key) { return entries.get(key); }
            public void put(K key, V value) { entries.put(key, value); }
        });
    }
}
