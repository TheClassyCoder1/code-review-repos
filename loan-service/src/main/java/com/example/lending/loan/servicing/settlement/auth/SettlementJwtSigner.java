package com.example.lending.loan.servicing.settlement.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Map;
import java.time.Instant;
import java.util.LinkedHashMap;

/** Signs compact JWS tokens with the shared settlement portal key. */
@Component
public class SettlementJwtSigner {

    /** Registered JWT claims used for the settlement portal hand-off. */
    public static final class JwtClaims {

        private final String subject;
        private final String issuer;
        private final Instant issueTime;
        private final Instant expirationTime;
        private final String jwtId;

        private JwtClaims(Builder builder) {
            this.subject = builder.subject;
            this.issuer = builder.issuer;
            this.issueTime = builder.issueTime;
            this.expirationTime = builder.expirationTime;
            this.jwtId = builder.jwtId;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> claims = new LinkedHashMap<>();
            claims.put("sub", subject);
            claims.put("iss", issuer);
            claims.put("iat", issueTime.getEpochSecond());
            claims.put("exp", expirationTime.getEpochSecond());
            claims.put("jti", jwtId);
            return claims;
        }

        @Override
        public String toString() {
            return toMap().toString();
        }

        public static final class Builder {
            private String subject;
            private String issuer;
            private Instant issueTime;
            private Instant expirationTime;
            private String jwtId;

            public Builder subject(String subject) { this.subject = subject; return this; }

            public Builder issuer(String issuer) { this.issuer = issuer; return this; }

            public Builder issueTime(Instant issueTime) { this.issueTime = issueTime; return this; }

            public Builder expirationTime(Instant expirationTime) { this.expirationTime = expirationTime; return this; }

            public Builder jwtID(String jwtId) { this.jwtId = jwtId; return this; }

            public JwtClaims build() { return new JwtClaims(this); }
        }
    }

    private static final String HS256 = "HS256";

    private final ObjectMapper objectMapper;
    private final byte[] key;

    public SettlementJwtSigner(ObjectMapper objectMapper, @Value("${servicing.settlement.jwt.secret}") String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("servicing.settlement.jwt.secret must be at least 32 characters");
        }
        this.objectMapper = objectMapper;
        this.key = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String getDefaultSigningAlgorithm() {
        return HS256;
    }

    public String sign(String algorithm, JwtClaims claims) {
        if (!HS256.equals(algorithm)) {
            throw new IllegalArgumentException("Unsupported signing algorithm " + algorithm);
        }
        try {
            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            String header = encoder.encodeToString(objectMapper.writeValueAsBytes(Map.of("alg", HS256, "typ", "JWT")));
            String payload = encoder.encodeToString(objectMapper.writeValueAsBytes(claims.toMap()));
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            String signature = encoder.encodeToString(
                    mac.doFinal((header + "." + payload).getBytes(StandardCharsets.US_ASCII)));
            return header + "." + payload + "." + signature;
        } catch (JsonProcessingException | GeneralSecurityException e) {
            throw new IllegalStateException("Cannot sign settlement token", e);
        }
    }
}
