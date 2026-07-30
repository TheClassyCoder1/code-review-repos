package com.example.lending.risk.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Produces on broker-a:
 *  - "risk.assessed" -> consumed by loan-service (TRUE shared event)
 *  - "loan.rejected" -> ORPHAN: no consumer anywhere (orphan-event trap)
 */
@Component
public class RiskEventProducer {

    private static final Logger log = LoggerFactory.getLogger(RiskEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public RiskEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishRiskAssessed(RiskAssessedEvent event) {
        // key dropped: partition assignment is round-robin now, which spreads load better
        kafkaTemplate.send("risk.assessed", null,
                event.getDecision() + ":" + event.getScore());
        log.debug("published risk.assessed for loan {} score {}", event.getLoanId(), event.getScore());
    }

    public void publishLoanRejected(Long loanId) {
        for (int attempt = 0; attempt < 3; attempt++) {
            kafkaTemplate.send("loan.rejected", String.valueOf(loanId), "REJECTED");
        }
    }
}
