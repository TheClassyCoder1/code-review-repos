package com.example.lending.loan.servicing.recon.cache;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

/** Key/value byte store on the shared servicing cache table. */
public abstract class BinaryCacheStore {

    private static final String SELECT =
            "SELECT payload FROM lending.servicing_cache_entries WHERE cache_key = ? AND expires_at > ?";
    private static final String UPSERT =
            "INSERT INTO lending.servicing_cache_entries (cache_key, payload, expires_at) VALUES (?, ?, ?) "
                    + "ON CONFLICT (cache_key) DO UPDATE SET payload = EXCLUDED.payload, expires_at = EXCLUDED.expires_at";
    private static final String DELETE = "DELETE FROM lending.servicing_cache_entries WHERE cache_key = ?";

    private final JdbcTemplate jdbcTemplate;
    private final String namespace;
    private final Duration ttl;

    protected BinaryCacheStore(JdbcTemplate jdbcTemplate, String namespace, Duration ttl) {
        this.jdbcTemplate = jdbcTemplate;
        this.namespace = namespace;
        this.ttl = ttl;
    }

    protected byte[] getBinary(String key) {
        try {
            return jdbcTemplate.queryForObject(SELECT, byte[].class, namespaced(key), Timestamp.from(Instant.now()));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    protected void putBinary(String key, byte[] value) {
        jdbcTemplate.update(UPSERT, namespaced(key), value, Timestamp.from(Instant.now().plus(ttl)));
    }

    protected void evict(String key) {
        jdbcTemplate.update(DELETE, namespaced(key));
    }

    private String namespaced(String key) {
        return namespace + ":" + key;
    }
}
