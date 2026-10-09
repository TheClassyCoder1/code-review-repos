package com.example.lending.loan.servicing.settlement.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/** Encrypts payout bank account numbers stored with settlement instructions. */
@Component
public class AccountNumberCipher {

    private static String key;

    public AccountNumberCipher(@Value("${servicing.settlement.account-key}") String accountKey) {
        AccountNumberCipher.key = accountKey;
    }

    public static String encryptHex(String content) {
        try {
            Cipher aes = Cipher.getInstance("AES");
            aes.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES"));
            String encryptResultStr = HexFormat.of().formatHex(aes.doFinal(content.getBytes(StandardCharsets.UTF_8)));
            return encryptResultStr;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot encrypt account number", e);
        }
    }

    public static String decryptHex(String encrypted) {
        try {
            Cipher aes = Cipher.getInstance("AES");
            aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES"));
            return new String(aes.doFinal(HexFormat.of().parseHex(encrypted)), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot decrypt account number", e);
        }
    }
}
