package com.example.lending.loan.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

@Component
@ConfigurationProperties(prefix = "webhooks.secrets")
public class WebhookSecrets {

    public static final String SIGNATURE_HEADER = "X-Partner-Signature";

    private Map<String, String> partners = new HashMap<>();

    public Map<String, String> getPartners() { return partners; }
    public void setPartners(Map<String, String> partners) { this.partners = partners; }

    public String sign(String partnerId, byte[] body) {
        String secret = partners.get(partnerId);
        if (secret == null) {
            throw new IllegalStateException("No webhook secret configured for partner");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not sign webhook payload", e);
        }
    }

    public boolean verify(String partnerId, String signature, byte[] body) {
        if (signature == null || !partners.containsKey(partnerId)) {
            return false;
        }
        return MessageDigest.isEqual(sign(partnerId, body).getBytes(StandardCharsets.US_ASCII),
                signature.trim().getBytes(StandardCharsets.US_ASCII));
    }
}
