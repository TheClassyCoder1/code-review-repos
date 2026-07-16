package com.example.lending.risk.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Produces on broker-a:
 *  - "risk.assessed" -> consumed by loan-service (TRUE shared event)
 *  - "loan.rejected" -> ORPHAN: no consumer anywhere (orphan-event trap)
 */
@Component
public class RiskEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public RiskEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishRiskAssessed(RiskAssessedEvent event) {
        kafkaTemplate.send("risk.assessed", String.valueOf(event.getLoanId()),
                event.getDecision() + ":" + event.getScore());
    }

    public void publishLoanRejected(Long loanId) {
        kafkaTemplate.send("loan.rejected", String.valueOf(loanId), "REJECTED");
    }
}
