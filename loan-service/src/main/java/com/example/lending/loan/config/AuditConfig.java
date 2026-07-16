package com.example.lending.loan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * BEAN-NAME COLLISION TRAP: bean "auditPublisher" is ALSO defined in risk-service,
 * but there it returns a DbAuditPublisher. Here it returns a KafkaAuditPublisher.
 * Same bean name, different implementation, different repo.
 */
@Configuration
public class AuditConfig {

    @Bean(name = "auditPublisher")
    public KafkaAuditPublisher auditPublisher() {
        return new KafkaAuditPublisher();
    }

    /** loan-service emits audit events to Kafka. */
    public static class KafkaAuditPublisher {
        public void publish(String entityType, String message) {
            // fixture: no-op emit
        }
    }
}
