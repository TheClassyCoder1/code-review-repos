package com.example.lending.platform.account.part6;

import com.example.lending.platform.account.kafka.AccountEventProducer;
import com.example.lending.platform.common.event.AccountCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Publishes "account.created" as a structured JSON document instead of the bare
 * account name, so downstream consumers get the account id, a display name and
 * the creation time in one message.
 *
 * Registered as the primary {@link AccountEventProducer} so AccountService picks
 * it up without any change to its wiring.
 */
@Primary
@Component
public class AccountCreatedJsonProducer extends AccountEventProducer {

    static final String TOPIC = "account.created";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public AccountCreatedJsonProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        super(kafkaTemplate);
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishAccountCreated(AccountCreatedEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("accountId", String.valueOf(event.getAccountId()));
        payload.put("displayName", event.getName());
        payload.put("createdAt", Instant.now().toString());
        kafkaTemplate.send(TOPIC, String.valueOf(event.getAccountId()), toJson(payload));
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not serialise account.created payload for account "
                    + payload.get("accountId"), e);
        }
    }
}
