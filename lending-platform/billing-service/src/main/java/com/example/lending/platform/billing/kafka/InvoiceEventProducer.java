package com.example.lending.platform.billing.kafka;

import com.example.lending.platform.billing.entity.Invoice;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Publishes invoice lifecycle events on broker-b. Records are keyed by account id so all
 * events for one account land on the same partition and are consumed in order.
 *
 * <p>Payload (v1): {@code {"version":1,"type":...,"invoiceId":...,"invoiceNumber":...,
 * "accountId":...,"status":...,"currency":...,"totalAmount":...,"outstandingAmount":...,
 * "occurredAt":...}}
 */
@Component
public class InvoiceEventProducer {

    public static final String TOPIC_ISSUED = "invoice.issued";
    public static final String TOPIC_PAID = "invoice.paid";
    public static final String TOPIC_OVERDUE = "invoice.overdue";

    private static final Logger log = LoggerFactory.getLogger(InvoiceEventProducer.class);
    private static final int PAYLOAD_VERSION = 1;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public InvoiceEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void invoiceIssued(Invoice invoice) {
        publish(TOPIC_ISSUED, "ISSUED", invoice);
    }

    public void invoicePaid(Invoice invoice) {
        publish(TOPIC_PAID, "PAID", invoice);
    }

    public void invoiceOverdue(Invoice invoice) {
        publish(TOPIC_OVERDUE, "OVERDUE", invoice);
    }

    private void publish(String topic, String type, Invoice invoice) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("version", PAYLOAD_VERSION);
        payload.put("type", type);
        payload.put("invoiceId", invoice.getId());
        payload.put("invoiceNumber", invoice.getInvoiceNumber());
        payload.put("accountId", invoice.getAccountId());
        payload.put("status", invoice.getStatus().name());
        payload.put("currency", invoice.getCurrency());
        payload.put("totalAmount", invoice.getTotalAmount());
        payload.put("outstandingAmount", invoice.outstandingAmount());
        payload.put("occurredAt", Instant.now().toString());
        try {
            kafkaTemplate.send(topic, String.valueOf(invoice.getAccountId()), objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not serialise " + type + " event for invoice " + invoice.getId(), e);
        }
        log.debug("published {} for invoice {}", type, invoice.getId());
    }
}
