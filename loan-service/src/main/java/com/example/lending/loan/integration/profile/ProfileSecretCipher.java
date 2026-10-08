package com.example.lending.loan.integration.profile;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.function.UnaryOperator;

/** AES-GCM codec for integration profile secrets. */
@Component
public class ProfileSecretCipher {

    private final SecretKeySpec key;

    public ProfileSecretCipher(@Value("${integration.profile.key}") String base64Key) {
        this.key = new SecretKeySpec(Base64.getDecoder().decode(base64Key), "AES");
    }

    public void decrypt(IntegrationProfile profile) {
        apply(profile, this::decryptValue);
    }

    private static void apply(IntegrationProfile profile, UnaryOperator<String> operator) {
        profile.getSecrets().replaceAll((name, value) -> value == null ? null : operator.apply(value));
    }

    private String decryptValue(String value) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(value));
            byte[] iv = new byte[12];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Could not decrypt profile secret", e);
        }
    }
}
