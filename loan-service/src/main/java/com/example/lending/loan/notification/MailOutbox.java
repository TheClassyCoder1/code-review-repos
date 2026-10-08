package com.example.lending.loan.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Map;

/** Queues templated e-mails; the mail relay picks up rows where sent_at is null. */
@Service
public class MailOutbox {

    private final OutboundMessageRepository messageRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public MailOutbox(OutboundMessageRepository messageRepository, ObjectMapper objectMapper, Clock clock) {
        this.messageRepository = messageRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void enqueue(String recipient, String template, Map<String, String> params) {
        OutboundMessage message = new OutboundMessage();
        message.setRecipient(recipient);
        message.setTemplate(template);
        message.setPayloadJson(toJson(params));
        message.setCreatedAt(clock.instant());
        messageRepository.save(message);
    }

    private String toJson(Map<String, String> params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Message parameters could not be serialised", e);
        }
    }
}
