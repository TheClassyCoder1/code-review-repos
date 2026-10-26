package com.example.lending.loan.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes topic "loan.applied" on BOTH brokers:
 *  - broker-a  -> TRUE shared (risk-service consumes it)
 *  - broker-b  -> FALSE match (same topic name, different broker, no consumer)
 */
@Component
public class LoanEventProducer {

    private static final Logger log = LoggerFactory.getLogger(LoanEventProducer.class);

    private static final String TOPIC = "loan.applied";

    private final KafkaTemplate<String, String> brokerATemplate;
    private final KafkaTemplate<String, String> brokerBTemplate;

    public LoanEventProducer(@Qualifier("brokerATemplate") KafkaTemplate<String, String> brokerATemplate,
                             @Qualifier("brokerBTemplate") KafkaTemplate<String, String> brokerBTemplate) {
        this.brokerATemplate = brokerATemplate;
        this.brokerBTemplate = brokerBTemplate;
    }

    public void publishLoanApplied(LoanAppliedEvent event) {
        String key = String.valueOf(event.getLoanId());
        String payload = event.getUserId() + ":" + event.getAmount();
        // consumed by risk-service (broker-a); a lost send leaves the loan stuck in APPLIED, so surface it
        brokerATemplate.send(TOPIC, key, payload).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("loan.applied publish failed for loanId={}", event.getLoanId(), ex);
            }
        });
        brokerBTemplate.send(TOPIC, key, payload); // FALSE match, no consumer (broker-b)
    }
}
