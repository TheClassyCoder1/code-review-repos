package com.example.lending.platform.account.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * BEAN-NAME COLLISION TRAP: bean "auditPublisher" also defined in billing-service,
 * different implementation. Same name, two modules, one repo.
 */
@Configuration
public class AuditConfig {

    @Bean(name = "auditPublisher")
    public LedgerAuditPublisher auditPublisher() {
        return new LedgerAuditPublisher();
    }

    /** account-service writes audit rows to the ledger. */
    public static class LedgerAuditPublisher {
        public void publish(String message) {
            // fixture: no-op
        }
    }
}
