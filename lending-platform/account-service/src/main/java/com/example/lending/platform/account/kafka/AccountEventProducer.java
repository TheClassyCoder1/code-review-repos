package com.example.lending.platform.account.kafka;

import com.example.lending.platform.common.event.AccountCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Produces "account.created" on broker-a (TRUE shared: notification-worker consumes it).
 * Uses the SHARED AccountCreatedEvent from platform-common.
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
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("accountId", event.getAccountId());
        payload.put("holderName", event.getName());
        payload.put("schema", "account.created/2");
        kafkaTemplate.send("account.created", String.valueOf(event.getAccountId()), toJson(payload));
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("account.created payload could not be serialized", e);
        }
    }
}
