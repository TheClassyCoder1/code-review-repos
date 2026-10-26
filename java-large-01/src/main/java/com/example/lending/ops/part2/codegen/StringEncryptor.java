package com.example.lending.ops.part2.codegen;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

/** Encrypts stored datasource passwords (AES-GCM, PBKDF2-derived key); key and salt come from the ops env file. */
@Component
public class StringEncryptor {
    private final String key;
    private final String hexSalt;

    public StringEncryptor(@Value("${ops.codegen.encryptor-key:}") String key, @Value("${ops.codegen.encryptor-salt:}") String hexSalt) {
        this.key = key;
        this.hexSalt = hexSalt;
    }

    public String encrypt(String plain) { return encryptor().encrypt(plain); }
    public String decrypt(String encrypted) { return encryptor().decrypt(encrypted); }

    private TextEncryptor encryptor() {
        if (key.isEmpty() || hexSalt.isEmpty()) {
            throw new IllegalStateException("ops.codegen.encryptor-key and -salt must be configured");
        }
        return Encryptors.delux(key, hexSalt);
    }
}
