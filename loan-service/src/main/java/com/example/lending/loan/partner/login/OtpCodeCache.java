package com.example.lending.loan.partner.login;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OtpCodeCache {

    private static final Duration TTL = Duration.ofMinutes(5);

    private record Entry(String code, Instant expiresAt) {
    }

    private final Map<String, Entry> codes = new ConcurrentHashMap<>();

    public void setAuthCode(String telephone, String code) {
        codes.put(telephone, new Entry(code, Instant.now().plus(TTL)));
    }

    /** Codes are single use: any attempt consumes the stored code. */
    public boolean consume(String telephone, String code) {
        Entry entry = codes.remove(telephone);
        return entry != null
                && code != null
                && Instant.now().isBefore(entry.expiresAt())
                && MessageDigest.isEqual(entry.code().getBytes(StandardCharsets.UTF_8), code.getBytes(StandardCharsets.UTF_8));
    }
}
