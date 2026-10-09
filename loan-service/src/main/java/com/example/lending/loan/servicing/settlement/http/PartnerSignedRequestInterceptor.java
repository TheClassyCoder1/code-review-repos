package com.example.lending.loan.servicing.settlement.http;

import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.Subject;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * Authenticates to the partner reporting service with an HMAC over principal, host and timestamp until the
 * service has issued its session cookie.
 */
public class PartnerSignedRequestInterceptor extends PartnerCookieRequestInterceptor {

    private final String serverPrincipal;
    private final String host;
    private final Subject loggedInSubject;
    private final byte[] signingKey;

    public PartnerSignedRequestInterceptor(
            String serverPrincipal,
            String host,
            Subject loggedInSubject,
            CookieStore cs,
            String cn,
            boolean isSSL,
            Map<String, String> additionalHeaders,
            Map<String, String> customCookies,
            byte[] signingKey) {
        super(cs, cn, isSSL, additionalHeaders, customCookies);
        this.serverPrincipal = serverPrincipal;
        this.host = host;
        this.loggedInSubject = loggedInSubject;
        this.signingKey = signingKey.clone();
    }

    @Override
    protected void addHttpAuthHeader(HttpRequest request, HttpContext httpContext) throws HttpException {
        String clientPrincipal = loggedInSubject.getPrincipals().stream()
                .map(Principal::getName)
                .findFirst()
                .orElseThrow(() -> new HttpException("No client principal in subject"));
        long timestamp = Instant.now().getEpochSecond();
        String data = clientPrincipal + "\n" + serverPrincipal + "\n" + host + "\n" + timestamp;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            String signature = Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
            request.setHeader("Authorization", "PartnerHMAC principal=\"" + clientPrincipal + "\", ts=\""
                    + timestamp + "\", sig=\"" + signature + "\"");
        } catch (GeneralSecurityException e) {
            throw new HttpException("Cannot sign partner request", e);
        }
    }
}
