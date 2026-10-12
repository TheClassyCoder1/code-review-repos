package com.example.lending.loan.servicing.recon.cache;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Arrays;

/**
 * Cache for large reconciliation results (matching reports). Values are split into chunks plus a {@link KeyHook}
 * index entry so that no single row exceeds the chunk size.
 */
@Component
public class ChunkedReconCache extends BinaryCacheStore {

    static final int CHUNK_SIZE = 256 * 1024;

    public ChunkedReconCache(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "recon-report", Duration.ofHours(6));
    }

    public void put(String key, byte[] value) {
        int chunks = Math.max(1, (value.length + CHUNK_SIZE - 1) / CHUNK_SIZE);
        String[] chunkKeys = new String[chunks];
        for (int i = 0; i < chunks; i++) {
            chunkKeys[i] = key + "#" + i;
            int from = i * CHUNK_SIZE;
            putBinary(chunkKeys[i], Arrays.copyOfRange(value, from, Math.min(value.length, from + CHUNK_SIZE)));
        }
        putBinary(key, SerializationSupport.serialize(new KeyHook(chunkKeys, value.length)));
    }

    public byte[] get(String key) {
        KeyHook hook = lookupKeyHook(key);
        if (hook == null) {
            return null;
        }
        ByteArrayOutputStream value = new ByteArrayOutputStream();
        for (String chunkKey : hook.getChunkKeys()) {
            byte[] chunk = getBinary(chunkKey);
            if (chunk == null) {
                return null;
            }
            value.writeBytes(chunk);
        }
        return value.size() == hook.getValueLength() ? value.toByteArray() : null;
    }

    public KeyHook lookupKeyHook(String keyS) {
        byte[] bytes = super.getBinary(keyS);
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return SerializationSupport.deserialize(bytes);
    }
}
