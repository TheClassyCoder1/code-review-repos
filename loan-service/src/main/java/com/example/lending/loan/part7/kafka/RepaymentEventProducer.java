package com.example.lending.loan.part7.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes "loan.repayment.scheduled" on broker-a. billing and notifications consume it to
 * pick up the new due dates.
 */
@Component
public class RepaymentEventProducer {

    private static final Logger log = LoggerFactory.getLogger(RepaymentEventProducer.class);
    private static final String TOPIC = "loan.repayment.scheduled";

    private final KafkaTemplate<String, String> brokerATemplate;

    public RepaymentEventProducer(@Qualifier("brokerATemplate") KafkaTemplate<String, String> brokerATemplate) {
        this.brokerATemplate = brokerATemplate;
    }

    public void publishScheduled(RepaymentScheduledEvent event) {
        String key = String.valueOf(event.getLoanId());
        brokerATemplate.send(TOPIC, key, event.toPayload())
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("failed to publish {} for loan {}", TOPIC, event.getLoanId(), ex);
                    }
                });
    }
}
