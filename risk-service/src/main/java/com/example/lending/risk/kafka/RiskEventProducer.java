package com.example.lending.risk.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

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

    /**
     * Publishes a rejection. The payload stays "REJECTED"; the reason travels as a record header so
     * existing readers of the topic see no format change.
     */
    public void publishRejection(Long loanId, String reasonCode) {
        ProducerRecord<String, String> record =
                new ProducerRecord<>("loan.rejected", String.valueOf(loanId), "REJECTED");
        record.headers().add("reason-code", reasonCode.getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record);
    }

    /** @deprecated use {@link #publishRejection(Long, String)}; kept so existing callers keep working. */
    @Deprecated
    public void publishLoanRejected(Long loanId) {
        publishRejection(loanId, "SCORE_ABOVE_THRESHOLD");
    }
}
