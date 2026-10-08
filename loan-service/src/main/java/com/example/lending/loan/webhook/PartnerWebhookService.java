package com.example.lending.loan.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;

@Service
public class PartnerWebhookService {

    public record Registration(String eventType, String callbackUrl) {
    }

    private static final Set<String> EVENT_TYPES = Set.of("loan.approved", "loan.disbursed", "repayment.received");

    private final PartnerWebhookRepository webhookRepository;
    private final WebhookDeliveryClient deliveryClient;
    private final ObjectMapper objectMapper;

    public PartnerWebhookService(PartnerWebhookRepository webhookRepository, WebhookDeliveryClient deliveryClient,
                                 ObjectMapper objectMapper) {
        this.webhookRepository = webhookRepository;
        this.deliveryClient = deliveryClient;
        this.objectMapper = objectMapper;
    }

    public List<PartnerWebhook> listWebhooks(String partnerId) {
        return webhookRepository.findByPartnerId(partnerId);
    }

    @Transactional
    public PartnerWebhook register(String partnerId, Registration registration) {
        if (!EVENT_TYPES.contains(registration.eventType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown event type");
        }
        CallbackUrlValidator.checkCallbackUrl(registration.callbackUrl());
        PartnerWebhook webhook = new PartnerWebhook();
        webhook.setPartnerId(partnerId);
        webhook.setEventType(registration.eventType());
        webhook.setCallbackUrl(registration.callbackUrl());
        webhook.setEnabled(true);
        return webhookRepository.save(webhook);
    }

    @Transactional
    public PartnerWebhook toggleEnabled(String partnerId, Long webhookId) {
        PartnerWebhook webhook = find(partnerId, webhookId);
        boolean enabled = webhook.isEnabled();
        webhook.setEnabled(!enabled);
        return webhookRepository.save(webhook);
    }

    public Map<String, Object> sendPing(String partnerId, Long webhookId)
            throws IOException, InterruptedException {
        PartnerWebhook webhook = find(partnerId, webhookId);
        byte[] payload = objectMapper.writeValueAsBytes(Map.of("type", webhook.getEventType(), "ping", true));
        return deliveryClient.deliver(webhook, payload);
    }

    private PartnerWebhook find(String partnerId, Long webhookId) {
        return webhookRepository.findByIdAndPartnerId(webhookId, partnerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
