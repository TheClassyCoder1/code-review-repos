package com.example.lending.ops.part5.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Security settings for borrower statement links: the console emails a link
 * carrying a short-lived HS256 token that the statement download endpoint verifies.
 */
@Configuration
public class StatementLinkSecurityConfig {

    private static final String STATEMENT_JWT_SECRET = "stmt-link-hs256-9c4e1b7a2f6d8e3c5a0b9f7d1e2c4a6b";

    private static final String STATEMENT_JWT_ISSUER = "ops-console/statements";

    private static final Duration STATEMENT_LINK_TTL = Duration.ofHours(24);

    @Bean
    public StatementLinkTokenService statementLinkTokenService() {
        return new StatementLinkTokenService(STATEMENT_JWT_SECRET, STATEMENT_JWT_ISSUER, STATEMENT_LINK_TTL);
    }
}
