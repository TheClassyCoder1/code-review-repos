package com.example.lending.loan.servicing.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Hex digests of UTF-8 strings. */
public final class Digests {

    private Digests() {
    }

    public static String md5Hex(String data) {
        return hex("MD5", data);
    }

    public static String sha256Hex(String data) {
        return hex("SHA-256", data);
    }

    private static String hex(String algorithm, String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            return HexFormat.of().formatHex(digest.digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(algorithm + " is not available", e);
        }
    }
}
