package com.example.lending.loan.kafka;

import com.example.lending.loan.service.LoanService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * TRUE shared event: consumes "risk.assessed" from broker-a (produced by risk-service).
 * Closes the loan<->risk event loop. Payload is "DECISION:score", keyed by loan id.
 */
@Component
public class RiskAssessedConsumer {

    private static final Logger log = LoggerFactory.getLogger(RiskAssessedConsumer.class);

    private final LoanService loanService;

    public RiskAssessedConsumer(LoanService loanService) {
        this.loanService = loanService;
    }

    @KafkaListener(topics = "risk.assessed", groupId = "loan")
    public void onRiskAssessed(ConsumerRecord<String, String> record) {
        Long loanId = parseLoanId(record.key());
        String value = record.value();
        if (loanId == null || value == null || !value.contains(":")) {
            log.warn("Skipping malformed risk.assessed record at offset {}", record.offset());
            return;
        }
        String decision = value.substring(0, value.indexOf(':'));
        boolean applied = loanService.recordDecision(loanId, decision);
        log.info("risk.assessed loanId={} decision={} applied={}", loanId, decision, applied);
    }

    private static Long parseLoanId(String key) {
        if (key == null) {
            return null;
        }
        try {
            return Long.valueOf(key);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
