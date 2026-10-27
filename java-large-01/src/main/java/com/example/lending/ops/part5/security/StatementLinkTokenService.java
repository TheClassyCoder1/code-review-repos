package com.example.lending.ops.part5.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/** Issues the HS256 tokens embedded in statement download links. */
public record StatementLinkTokenService(String secret, String issuer, Duration ttl) {

    private static final String HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    public String issue(long statementId) {
        long exp = Instant.now().plus(ttl).getEpochSecond();
        String claims = "{\"iss\":\"" + issuer + "\",\"stmt\":" + statementId + ",\"exp\":" + exp + "}";
        String signingInput = b64(HEADER.getBytes(StandardCharsets.UTF_8)) + "."
                + b64(claims.getBytes(StandardCharsets.UTF_8));
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return signingInput + "." + b64(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HS256 unavailable", e);
        }
    }

    @Override
    public String toString() {
        return "StatementLinkTokenService[issuer=" + issuer + ", ttl=" + ttl + "]";
    }

    private static String b64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
