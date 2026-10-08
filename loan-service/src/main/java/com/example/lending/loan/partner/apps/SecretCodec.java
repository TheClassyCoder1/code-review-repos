package com.example.lending.loan.partner.apps;

import com.example.lending.loan.support.crypto.DesCipher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecretCodec {

    private final String secretKey;

    public SecretCodec(@Value("${partner.apps.secret-key}") String secretKey) {
        this.secretKey = secretKey;
    }

    public void encoderSecret(PartnerApp application) {
        try {
            application.setClientSecret(DesCipher.encrypt(application.getClientSecret(), secretKey));
        } catch (Exception e) {
            throw new IllegalStateException("Could not encrypt client secret", e);
        }
    }

    public void decoderSecret(PartnerApp application) {
        try {
            application.setClientSecret(DesCipher.decrypt(application.getClientSecret(), secretKey));
        } catch (Exception e) {
            throw new IllegalStateException("Could not decrypt client secret", e);
        }
    }
}
