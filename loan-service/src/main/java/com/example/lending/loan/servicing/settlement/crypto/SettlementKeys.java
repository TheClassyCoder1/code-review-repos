package com.example.lending.loan.servicing.settlement.crypto;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;

/** Key material for settlement partners. */
public final class SettlementKeys {

    static final String ENCRYPTION_KEY_SPEC = "AES";

    private SettlementKeys() {
    }

    public static byte[] generateNewSecretKey() {
        // Get the KeyGenerator
        KeyGenerator kgen;
        try {
            kgen = KeyGenerator.getInstance(ENCRYPTION_KEY_SPEC);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("could not generate cipher key", e);
        }
        kgen.init(128);

        // Generate the secret key specs.
        SecretKey skey = kgen.generateKey();

        return skey.getEncoded();
    }
}
