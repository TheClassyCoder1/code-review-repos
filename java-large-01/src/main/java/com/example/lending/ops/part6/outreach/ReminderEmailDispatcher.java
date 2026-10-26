package com.example.lending.ops.part6.outreach;

import com.example.lending.ops.part6.common.PiiMask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Queues reminder emails on the outbox table drained by the mail relay. The relay owns delivery,
 * retries and bounce handling; this class only decides what goes on the queue.
 */
@Slf4j
@Component
public class ReminderEmailDispatcher {

    private final JdbcTemplate jdbcTemplate;
    private final String senderAddress;

    public ReminderEmailDispatcher(JdbcTemplate jdbcTemplate,
                                   @Value("${ops.outreach.sender-address:collections@lender.example}") String senderAddress) {
        this.jdbcTemplate = jdbcTemplate;
        this.senderAddress = senderAddress;
    }

    public boolean queue(CustomerContact contact, String subject, String body) {
        if (!contact.isEmailOptIn()) {
            log.debug("customer {} has not opted in to email; reminder skipped", contact.getCustomerId());
            return false;
        }
        if (contact.getEmail() == null || contact.getEmail().isBlank()) {
            log.warn("customer {} has no email on file; reminder skipped", contact.getCustomerId());
            return false;
        }
        jdbcTemplate.update("INSERT INTO outreach_outbox (customer_id, channel, sender, recipient, subject, body, "
                        + "queued_at) VALUES (?, 'EMAIL', ?, ?, ?, ?, ?)",
                contact.getCustomerId(), senderAddress, contact.getEmail(), subject, body,
                Timestamp.from(Instant.now()));
        log.info("reminder email queued for customer {} to {}", contact.getCustomerId(), PiiMask.email(contact.getEmail()));
        return true;
    }

    public boolean queueSms(CustomerContact contact, String text) {
        if (!contact.isSmsOptIn() || contact.getPhone() == null) {
            return false;
        }
        jdbcTemplate.update("INSERT INTO outreach_outbox (customer_id, channel, sender, recipient, subject, body, "
                        + "queued_at) VALUES (?, 'SMS', 'LENDER', ?, NULL, ?, ?)",
                contact.getCustomerId(), contact.getPhone(), text, Timestamp.from(Instant.now()));
        log.info("reminder sms queued for customer {}", contact.getCustomerId());
        return true;
    }
}
