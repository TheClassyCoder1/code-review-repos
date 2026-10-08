package com.example.lending.loan.partner.account;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import java.security.NoSuchAlgorithmException;

/**
 * Fails startup when the JVM cannot provide the password hashing algorithm, so the service
 * never runs with a provider set that would make hashing unavailable.
 */
@Component
public class PasswordHashingCheck {

    @PostConstruct
    void verifyAlgorithmAvailable() throws NoSuchAlgorithmException {
        SecretKeyFactory.getInstance(CryptoUtil.PBKDF2_ALGORITHM);
    }
}
