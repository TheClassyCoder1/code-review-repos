package com.example.lending.platform.account.kafka;

import com.example.lending.platform.common.event.AccountCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Produces "account.created" on broker-a (TRUE shared: notification-worker consumes it).
 * Uses the SHARED AccountCreatedEvent from platform-common, serialized as JSON so the
 * account id travels in the value as well as the key.
 */
@Component
public class AccountEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public AccountEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishAccountCreated(AccountCreatedEvent event) {
        kafkaTemplate.send("account.created", String.valueOf(event.getAccountId()), toJson(event));
    }

    private String toJson(AccountCreatedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not serialize account.created for account " + event.getAccountId(), e);
        }
    }
}
