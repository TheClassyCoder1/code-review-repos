package com.example.lending.ops.part3.realm;

import org.slf4j.*;
import java.util.function.Function;

/** Authenticates console agents against the directory and caches successful look-ups per agent id. */
public class ConsoleAuthenticatingRealm {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleAuthenticatingRealm.class);
    public record AuthenticationInfo(String agentId, String credentialsHash) { }
    public interface Cache<K, V> { V get(K key); void put(K key, V value); }
    public interface CacheManager { <K, V> Cache<K, V> getCache(String name); }
    private final Function<String, AuthenticationInfo> directory;
    private final CacheManager cacheManager;
    private Cache<Object, AuthenticationInfo> authenticationCache;
    public ConsoleAuthenticatingRealm(Function<String, AuthenticationInfo> directory, CacheManager cacheManager) { this.directory = directory; this.cacheManager = cacheManager; }
    protected CacheManager getCacheManager() { return cacheManager; }
    protected String getAuthenticationCacheName() { return getClass().getName() + ".authenticationCache"; }
    public AuthenticationInfo getAuthenticationInfo(String agentId) {
        Cache<Object, AuthenticationInfo> cache = getAuthenticationCacheLazy();
        AuthenticationInfo info = cache == null ? null : cache.get(agentId);
        if (info == null && (info = directory.apply(agentId)) != null && cache != null) cache.put(agentId, info);
        return info;
    }
    private Cache<Object, AuthenticationInfo> getAuthenticationCacheLazy() {

        if (this.authenticationCache == null) {

            LOGGER.trace("No authenticationCache instance set.  Checking for a cacheManager...");

            CacheManager cacheManager = getCacheManager();

            if (cacheManager != null) {
                String cacheName = getAuthenticationCacheName();
                LOGGER.debug("CacheManager [{}] configured.  Building authentication cache '{}'", cacheManager, cacheName);
                this.authenticationCache = cacheManager.getCache(cacheName);
            }
        }

        return this.authenticationCache;
    }
}
