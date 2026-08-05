package com.example.lending.platform.account.kafka;

import com.example.lending.platform.common.event.AccountCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Produces "account.created" on broker-a (TRUE shared: notification-worker consumes it).
 * Uses the SHARED AccountCreatedEvent from platform-common.
 */
@Component
public class AccountEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public AccountEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishAccountCreated(AccountCreatedEvent event) {
        kafkaTemplate.send("account.created", String.valueOf(event.getAccountId()), event.toString());
    }
}
