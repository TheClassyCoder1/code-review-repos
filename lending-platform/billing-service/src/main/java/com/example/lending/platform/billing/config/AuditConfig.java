package com.example.lending.platform.billing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * BEAN-NAME COLLISION TRAP: bean "auditPublisher" also defined in account-service
 * (LedgerAuditPublisher). Here it returns an InvoiceAuditPublisher.
 */
@Configuration
public class AuditConfig {

    @Bean(name = "auditPublisher")
    public InvoiceAuditPublisher auditPublisher() {
        return new InvoiceAuditPublisher();
    }

    /** billing-service emits audit events onto invoices. */
    public static class InvoiceAuditPublisher {
        public void publish(String message) {
            // fixture: no-op
        }
    }
}
