package com.example.lending.loan.partner.openapi;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenApiAuthInterceptor implements HandlerInterceptor {

    private static final long MAX_SKEW_MILLIS = Duration.ofMinutes(5).toMillis();

    private final PartnerApiCredentialRepository credentialRepository;
    private final ApiSignatureVerifier signatureVerifier;

    public OpenApiAuthInterceptor(PartnerApiCredentialRepository credentialRepository,
                                  ApiSignatureVerifier signatureVerifier) {
        this.credentialRepository = credentialRepository;
        this.signatureVerifier = signatureVerifier;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String apiKey = request.getHeader("X-Api-Key");
        PartnerApiCredential credential = apiKey == null ? null : credentialRepository.findById(apiKey).orElse(null);
        if (credential == null || !freshTimestamp(request.getParameter("timestamp"))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        Map<String, List<String>> params = new HashMap<>();
        request.getParameterMap().forEach((name, values) -> params.put(name, Arrays.asList(values)));
        if (!signatureVerifier.verify(apiKey, params, credential.getSecret(), request.getHeader("X-Signature"))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        request.setAttribute(OpenApiCaller.ATTRIBUTE, new OpenApiCaller(apiKey, credential.getTenantId()));
        return true;
    }

    private static boolean freshTimestamp(String timestamp) {
        try {
            return Math.abs(System.currentTimeMillis() - Long.parseLong(timestamp)) <= MAX_SKEW_MILLIS;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
