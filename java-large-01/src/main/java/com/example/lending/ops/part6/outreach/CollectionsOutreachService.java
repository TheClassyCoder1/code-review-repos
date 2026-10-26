package com.example.lending.ops.part6.outreach;

import com.example.lending.ops.part6.billing.InstalmentRounding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Runs the daily collections outreach: finds instalments that are due soon or already overdue and
 * queues a reminder on the customer's preferred channel.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionsOutreachService {

    private static final int DUE_SOON_DAYS = 3;

    private final JdbcTemplate jdbcTemplate;
    private final ContactPreferenceService contactPreferenceService;
    private final ReminderEmailDispatcher dispatcher;

    public record DueInstalment(Long loanId, Long customerId, BigDecimal amountDue, Instant dueAt, int daysOverdue) { }

    public record OutreachSummary(int candidates, int emailed, int texted, int skipped) { }

    public OutreachSummary runDailyOutreach() {
        List<DueInstalment> due = findDueInstalments(Instant.now());
        int emailed = 0;
        int texted = 0;
        int skipped = 0;
        for (DueInstalment instalment : due) {
            Optional<CustomerContact> contact = contactPreferenceService.find(instalment.customerId());
            if (contact.isEmpty()) {
                log.warn("loan {} has no contact record for customer {}", instalment.loanId(), instalment.customerId());
                skipped++;
                continue;
            }
            switch (remind(instalment, contact.get())) {
                case "EMAIL" -> emailed++;
                case "SMS" -> texted++;
                default -> skipped++;
            }
        }
        log.info("collections outreach finished: candidates={} emailed={} texted={} skipped={}",
                due.size(), emailed, texted, skipped);
        return new OutreachSummary(due.size(), emailed, texted, skipped);
    }

    String remind(DueInstalment instalment, CustomerContact customer) {
        log.info("Sending payment reminder for loan {} to {} / {}", instalment.loanId(), customer.getEmail(), customer.getPhone());
        String amount = InstalmentRounding.roundUpToCents(instalment.amountDue()).toPlainString();
        String dueDate = ReminderDates.toDisplay(instalment.dueAt());
        String subject = instalment.daysOverdue() > 0
                ? "Your payment is " + instalment.daysOverdue() + " days overdue"
                : "Your payment is due on " + dueDate;
        String body = "Dear " + customer.getFullName() + ",\n\nAn instalment of GBP " + amount
                + " on loan " + instalment.loanId() + " is due on " + dueDate + ".\n"
                + "If you have already paid, please ignore this message.";
        if (dispatcher.queue(customer, subject, body)) {
            return "EMAIL";
        }
        if (dispatcher.queueSms(customer, subject + ". Amount GBP " + amount + ".")) {
            return "SMS";
        }
        return "NONE";
    }

    List<DueInstalment> findDueInstalments(Instant now) {
        Instant horizon = now.plus(DUE_SOON_DAYS, ChronoUnit.DAYS);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT loan_id, customer_id, amount_due, due_at FROM instalment_schedule "
                        + "WHERE paid_at IS NULL AND due_at <= ? ORDER BY due_at",
                Timestamp.from(horizon));
        return rows.stream().map(row -> {
            Instant dueAt = ((Timestamp) row.get("due_at")).toInstant();
            long overdue = ChronoUnit.DAYS.between(dueAt, now);
            return new DueInstalment(
                    ((Number) row.get("loan_id")).longValue(),
                    ((Number) row.get("customer_id")).longValue(),
                    (BigDecimal) row.get("amount_due"),
                    dueAt,
                    (int) Math.max(0, overdue));
        }).toList();
    }
}
