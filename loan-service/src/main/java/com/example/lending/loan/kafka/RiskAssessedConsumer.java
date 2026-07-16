package com.example.lending.loan.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * TRUE shared event: consumes "risk.assessed" from broker-a (produced by risk-service).
 * Closes the loan<->risk event loop.
 */
@Component
public class RiskAssessedConsumer {

    @KafkaListener(topics = "risk.assessed", groupId = "loan")
    public void onRiskAssessed(String message) {
        // fixture: would update loan status from the assessment result
    }
}
