package com.example.lending.loan.partner;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * HMAC-SHA256 signatures for credit bureau webhooks. {@link #fingerprint(String)} re-keys a value
 * with a per-process random key so signatures can be compared without exposing their bytes.
 */
@Component
public class PayloadSigner {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final SecretKeySpec signingKey;
    private final SecretKeySpec fingerprintKey;

    public PayloadSigner(@Value("${lending.partners.bureau.signing-secret}") String signingSecret) {
        this.signingKey = new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        byte[] randomKey = new byte[32];
        new SecureRandom().nextBytes(randomKey);
        this.fingerprintKey = new SecretKeySpec(randomKey, HMAC_SHA256);
    }

    public String sign(byte[] payload) {
        return HexFormat.of().formatHex(hmac(signingKey, payload));
    }

    public String fingerprint(String value) {
        return HexFormat.of().formatHex(hmac(fingerprintKey, value.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] hmac(SecretKeySpec key, byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(key);
            return mac.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 not available", e);
        }
    }
}
