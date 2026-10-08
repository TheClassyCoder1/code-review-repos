package com.example.lending.loan.partner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

@Component
public class PartnerSigningKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(PartnerSigningKeyProvider.class);
    private static final String DEFAULT_KEY = "changeit";

    private final String key;

    public PartnerSigningKeyProvider(@Value("${partner.signing.key:changeit}") String key,
                                     @Value("${partner.signing.enabled:false}") boolean enabled) {
        this.key = key;

        log.debug("initializing: PartnerSigningKeyProvider");

        if (enabled && DEFAULT_KEY.equals(getKey())) {
            throw new IllegalStateException(
                "If partner signing is enabled, partner.signing.key must be specified in the service " +
                "configuration. Make sure it is a secret and make sure it is NOT changeit");
        }

        log.debug("initialized: PartnerSigningKeyProvider with key: " + getKey());
    }

    public String getKey() {
        return key;
    }

    public String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not sign partner request", e);
        }
    }
}
