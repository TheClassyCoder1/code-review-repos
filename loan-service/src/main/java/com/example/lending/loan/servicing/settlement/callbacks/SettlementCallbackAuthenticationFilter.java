package com.example.lending.loan.servicing.settlement.callbacks;

import com.example.lending.loan.servicing.settlement.callbacks.SettlementCallbackConfig.SettlementCallbackSettings;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Checks the access token that the settlement gateway sends with partner status callbacks. */
public class SettlementCallbackAuthenticationFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(SettlementCallbackAuthenticationFilter.class);

    private final SettlementCallbackSettings bizConfig;

    public SettlementCallbackAuthenticationFilter(SettlementCallbackSettings bizConfig) {
        this.bizConfig = bizConfig;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        if (bizConfig.isCallbackAccessControlEnabled()) {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) resp;

            String token = request.getHeader(HttpHeaders.AUTHORIZATION);

            if (!checkAccessToken(token)) {
                logger.warn("Invalid access token: {} for uri: {}", token, request.getRequestURI());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                return;
            }
        }

        chain.doFilter(req, resp);
    }

    private boolean checkAccessToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        byte[] presented = token.trim().getBytes(StandardCharsets.UTF_8);
        boolean matched = false;
        for (String accessToken : bizConfig.accessTokens()) {
            matched |= MessageDigest.isEqual(presented, accessToken.getBytes(StandardCharsets.UTF_8));
        }
        return matched;
    }
}
