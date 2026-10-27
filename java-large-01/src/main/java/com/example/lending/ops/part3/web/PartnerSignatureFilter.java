package com.example.lending.ops.part3.web;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.*;
import java.util.HexFormat;

/** Verifies the HMAC partners put on callback requests before the controller reads the body. */
@Component
public class PartnerSignatureFilter extends OncePerRequestFilter {
    private final SecretKeySpec key;
    public PartnerSignatureFilter(@Value("${ops.partner.secret-key}") String secret) { key = new SecretKeySpec(secret.getBytes(), "HmacSHA256"); }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) { return !request.getRequestURI().startsWith("/api/ops/partner/callbacks"); }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        RepeatBodyRequestWrapper wrapped = new RepeatBodyRequestWrapper(request);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            if (MessageDigest.isEqual(mac.doFinal(wrapped.getBodyBytes()), HexFormat.of().parseHex(request.getHeader("X-Partner-Signature")))) {
                chain.doFilter(wrapped, response);
                return;
            }
        } catch (GeneralSecurityException | IllegalArgumentException | NullPointerException e) {
            // malformed or missing signature: rejected below
        }
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "invalid partner signature");
    }
}
