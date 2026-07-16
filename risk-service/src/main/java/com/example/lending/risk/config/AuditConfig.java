package com.example.lending.risk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * BEAN-NAME COLLISION TRAP: bean "auditPublisher" is ALSO defined in loan-service,
 * but there it returns a KafkaAuditPublisher. Here it returns a DbAuditPublisher.
 * Same bean name, different implementation, different repo.
 */
@Configuration
public class AuditConfig {

    @Bean(name = "auditPublisher")
    public DbAuditPublisher auditPublisher() {
        return new DbAuditPublisher();
    }

    /** risk-service writes audit rows to the DB. */
    public static class DbAuditPublisher {
        public void publish(String entityType, String message) {
            // fixture: no-op persistence
        }
    }
}
