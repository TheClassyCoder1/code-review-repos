package com.example.lending.loan.servicing.security.portal;

import com.example.lending.loan.servicing.common.ServicingException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

/** Signed access tokens for the borrower statement portal: {@code base64(username).expiry.hmac}. */
@Component
public class PortalTokens {

    public static final String HEADER = "X-Access-Token";

    private static final String HMAC = "HmacSHA256";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final byte[] secret;
    private final Clock clock = Clock.systemUTC();

    public PortalTokens(@Value("${servicing.portal.token-secret}") String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("servicing.portal.token-secret must be at least 32 characters");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String issue(String username) {
        String user = Base64.getUrlEncoder().withoutPadding().encodeToString(username.getBytes(StandardCharsets.UTF_8));
        long expiresAt = clock.instant().plus(TTL).getEpochSecond();
        String payload = user + "." + expiresAt;
        return payload + "." + sign(payload);
    }

    /** Returns the username of a valid, unexpired token, or {@code null}. */
    public String getUsername(String token) {
        if (token == null) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }
        String payload = parts[0] + "." + parts[1];
        byte[] expected = sign(payload).getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expected, parts[2].getBytes(StandardCharsets.US_ASCII))) {
            return null;
        }
        long expiresAt;
        try {
            expiresAt = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
        if (clock.instant().getEpochSecond() > expiresAt) {
            return null;
        }
        try {
            return new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String getUserNameByToken(HttpServletRequest request) throws ServicingException {
        String accessToken = request.getHeader(HEADER);
        String username = getUsername(accessToken);
        if (username == null || username.isEmpty()) {
            throw new ServicingException(HttpStatus.UNAUTHORIZED, "No portal user for access token");
        }
        return username;
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(secret, HMAC));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot sign portal token", e);
        }
    }
}
