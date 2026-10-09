package com.example.lending.loan.servicing.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/** Queues e-mails on the notification topic; the notification service renders and sends them. */
@Component
public class ServicingMailer {

    static final String TOPIC = "servicing.mail.requested";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public ServicingMailer(@Qualifier("brokerATemplate") KafkaTemplate<String, String> kafkaTemplate,
                           ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void send(String to, String subject, String body) {
        String messageId = UUID.randomUUID().toString();
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "messageId", messageId, "to", to, "subject", subject, "body", body));
            kafkaTemplate.send(TOPIC, messageId, payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize mail request " + messageId, e);
        }
    }
}
