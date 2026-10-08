package com.example.lending.loan.ops;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** One-time token that lets an operator on the host create the first ops administrator. */
public final class OpsBootstrapSecurity {

    private static final Logger LOG = LoggerFactory.getLogger(OpsBootstrapSecurity.class);
    private static final long LIFETIME_MS = 60L * 60L * 1000L;

    private static boolean completed;
    private static String digest;
    private static long expires;

    private OpsBootstrapSecurity() {
    }

    public static synchronized void start() {
        if (completed || digest != null) {
            return;
        }
        byte[] raw = new byte[32];
        new SecureRandom().nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        digest = sha256(token);
        expires = System.currentTimeMillis() + LIFETIME_MS;
        LOG.warn(box(
                "LOAN SERVICE OPS SETUP REQUIRED",
                "",
                "No ops administrator exists yet.",
                "Call POST /internal/ops/setup from this host to create one.",
                "",
                "Enter this one-time setup token (expires in 60 minutes):",
                token));
    }

    public static synchronized boolean redeem(String token) {
        if (completed || digest == null || token == null || System.currentTimeMillis() > expires) {
            return false;
        }
        boolean match = MessageDigest.isEqual(digest.getBytes(StandardCharsets.US_ASCII),
                sha256(token).getBytes(StandardCharsets.US_ASCII));
        if (match) {
            completed = true;
            digest = null;
        }
        return match;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String box(String... lines) {
        StringBuilder sb = new StringBuilder(System.lineSeparator());
        for (String line : lines) {
            sb.append("  | ").append(line).append(System.lineSeparator());
        }
        return sb.toString();
    }
}
