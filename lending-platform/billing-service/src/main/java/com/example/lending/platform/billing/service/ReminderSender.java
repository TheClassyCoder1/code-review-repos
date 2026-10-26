package com.example.lending.platform.billing.service;

import com.example.lending.platform.billing.entity.Invoice;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hands overdue reminders to notification-worker. Billing only knows the account id; the worker
 * resolves the borrower's contact details and preferred channel itself.
 */
@Component
public class ReminderSender {

    static final String TOPIC = "billing.reminder.requested";

    private static final Logger log = LoggerFactory.getLogger(ReminderSender.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public ReminderSender(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendOverdueReminder(Invoice invoice, long daysOverdue) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("version", 1);
        payload.put("template", templateFor(invoice.getReminderCount()));
        payload.put("accountId", invoice.getAccountId());
        payload.put("invoiceId", invoice.getId());
        payload.put("invoiceNumber", invoice.getInvoiceNumber());
        payload.put("currency", invoice.getCurrency());
        payload.put("outstandingAmount", invoice.outstandingAmount());
        payload.put("dueDate", invoice.getDueDate().toString());
        payload.put("daysOverdue", daysOverdue);
        try {
            kafkaTemplate.send(TOPIC, String.valueOf(invoice.getAccountId()), objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not serialise reminder for invoice " + invoice.getId(), e);
        }
        log.info("reminder {} requested for invoice {}", invoice.getReminderCount() + 1, invoice.getId());
    }

    static String templateFor(int remindersAlreadySent) {
        switch (remindersAlreadySent) {
            case 0:
                return "overdue-friendly";
            case 1:
                return "overdue-firm";
            default:
                return "overdue-final";
        }
    }
}
