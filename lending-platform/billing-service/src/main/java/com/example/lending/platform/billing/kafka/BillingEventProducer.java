package com.example.lending.platform.billing.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * FALSE MATCH: produces topic "account.created" on broker-b — SAME topic name as
 * account-service (broker-a), but a different broker and no shared consumer.
 * Not the same event stream as the real account.created on broker-a.
 */
@Component
public class BillingEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public BillingEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishAccountCreated(Long accountId, String name) {
        kafkaTemplate.send("account.created", String.valueOf(accountId), name);
    }
}
