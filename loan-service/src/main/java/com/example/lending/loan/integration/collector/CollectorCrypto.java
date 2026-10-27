package com.example.lending.loan.integration.collector;

import java.util.concurrent.atomic.AtomicReference;

/** Holds the AES key the manager hands to bureau data collectors when they come online. */
public final class CollectorCrypto {

    private static final AtomicReference<String> DEFAULT_SECRET_KEY = new AtomicReference<>();

    private CollectorCrypto() {
    }

    public static void setDefaultSecretKey(String secretKey) {
        DEFAULT_SECRET_KEY.set(secretKey);
    }

    public static String defaultSecretKey() {
        return DEFAULT_SECRET_KEY.get();
    }
}
