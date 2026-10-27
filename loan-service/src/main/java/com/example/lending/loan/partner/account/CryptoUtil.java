package com.example.lending.loan.partner.account;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public final class CryptoUtil {

    static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "{PBKDF2}";
    private static final int ITERATIONS = 310_000;
    private static final int KEY_LENGTH = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private CryptoUtil() {
    }

    public static String getPbkdf2SaltedPassword(byte[] password) throws NoSuchAlgorithmException {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        PBEKeySpec spec = new PBEKeySpec(new String(password, StandardCharsets.UTF_8).toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        try {
            byte[] hash = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).getEncoded();
            Base64.Encoder encoder = Base64.getEncoder();
            return PREFIX + ITERATIONS + "$" + encoder.encodeToString(salt) + "$" + encoder.encodeToString(hash);
        } catch (InvalidKeySpecException e) {
            throw new IllegalStateException(e);
        } finally {
            spec.clearPassword();
        }
    }
}
