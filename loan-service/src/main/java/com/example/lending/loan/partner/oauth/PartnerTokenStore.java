package com.example.lending.loan.partner.oauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PartnerTokenStore {

    private static final Logger _logger = LoggerFactory.getLogger(PartnerTokenStore.class);

    private static final String AUTH = "auth:";

    private final TokenCache tokenCache;

    public PartnerTokenStore(TokenCache tokenCache) {
        this.tokenCache = tokenCache;
    }

    private TokenCache.Connection getConnection() {
        return tokenCache.connect();
    }

    public void storeAuthentication(String token, PartnerAuthentication authentication) {
        TokenCache.Connection conn = getConnection();
        try {
            conn.set(AUTH + token, authentication);
        } finally {
            conn.close();
        }
    }

    public PartnerAuthentication readAuthentication(String token) {
        _logger.trace("read Authentication by token " + token + " , token key " + AUTH + token);
        TokenCache.Connection conn = getConnection();
        try {
            PartnerAuthentication auth = conn.getObject(AUTH + token);
            return auth;
        } finally {
            conn.close();
        }
    }
}
