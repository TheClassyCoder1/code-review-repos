package com.example.lending.ops.part2.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;

/**
 * Encodes agent-session values for the shared cache as {@code HMAC-SHA256(32 bytes) || payload}.
 * Whoever can write to the cache cannot forge a tag without the server key, so {@link #decode} only
 * deserializes payloads this service wrote; the tag is checked in constant time before any payload
 * byte is interpreted.
 */
@Component
@ConditionalOnProperty("ops.cache.signing-key")
public class SignedCacheCodec {

    private final SecretKeySpec key;
    private final SaJdkSerializer serializer = new SaJdkSerializer();

    public SignedCacheCodec(@Value("${ops.cache.signing-key}") String signingKey) {
        this.key = new SecretKeySpec(signingKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public byte[] encode(Serializable value) throws GeneralSecurityException {
        byte[] payload = serializer.valueToBytes(value);
        byte[] out = Arrays.copyOf(tag(payload), 32 + payload.length);
        System.arraycopy(payload, 0, out, 32, payload.length);
        return out;
    }

    public Object decode(byte[] stored) throws GeneralSecurityException {
        byte[] payload = Arrays.copyOfRange(stored, 32, stored.length);
        if (!MessageDigest.isEqual(Arrays.copyOf(stored, 32), tag(payload))) {
            throw new SecurityException("cache entry signature mismatch");
        }
        return serializer.valueFromBytes(payload);
    }

    private byte[] tag(byte[] payload) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        return mac.doFinal(payload);
    }
}
