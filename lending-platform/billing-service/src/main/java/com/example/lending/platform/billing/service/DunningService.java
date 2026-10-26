package com.example.lending.platform.billing.service;

import com.example.lending.platform.billing.entity.Invoice;
import com.example.lending.platform.billing.entity.InvoiceLine;
import com.example.lending.platform.billing.entity.InvoiceStatus;
import com.example.lending.platform.billing.kafka.InvoiceEventProducer;
import com.example.lending.platform.billing.repository.InvoiceRepository;
import com.example.lending.platform.common.util.MoneyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;

/**
 * Daily collections pass over unpaid invoices:
 * <ol>
 *   <li>ISSUED invoices past their due date move to OVERDUE and an {@code invoice.overdue} event is sent;</li>
 *   <li>a one-off late fee is posted once the grace period is over;</li>
 *   <li>reminders follow the configured cadence, capped at {@code billing.dunning.max-reminders}.</li>
 * </ol>
 * Disputed invoices are left alone until the dispute is resolved.
 */
@Service
public class DunningService {

    private static final Logger log = LoggerFactory.getLogger(DunningService.class);
    private static final String LATE_FEE_KIND = "LATE_FEE";

    private final InvoiceRepository invoiceRepository;
    private final InvoiceEventProducer eventProducer;
    private final ReminderSender reminderSender;
    private final Clock clock;
    private final int gracePeriodDays;
    private final double lateFeeRate;
    private final double lateFeeCap;
    private final int maxReminders;
    private final Duration reminderInterval;

    public DunningService(InvoiceRepository invoiceRepository,
                          InvoiceEventProducer eventProducer,
                          ReminderSender reminderSender,
                          Clock billingClock,
                          @Value("${billing.dunning.grace-period-days:5}") int gracePeriodDays,
                          @Value("${billing.dunning.late-fee-rate:0.02}") double lateFeeRate,
                          @Value("${billing.dunning.late-fee-cap:25}") double lateFeeCap,
                          @Value("${billing.dunning.max-reminders:3}") int maxReminders,
                          @Value("${billing.dunning.reminder-interval-days:7}") int reminderIntervalDays) {
        this.invoiceRepository = invoiceRepository;
        this.eventProducer = eventProducer;
        this.reminderSender = reminderSender;
        this.clock = billingClock;
        this.gracePeriodDays = gracePeriodDays;
        this.lateFeeRate = lateFeeRate;
        this.lateFeeCap = lateFeeCap;
        this.maxReminders = maxReminders;
        this.reminderInterval = Duration.ofDays(reminderIntervalDays);
    }

    /** Runs one collections pass and returns how many invoices were touched. */
    @Transactional
    public DunningRun run() {
        LocalDate today = LocalDate.now(clock);
        Instant now = clock.instant();
        List<Invoice> candidates = invoiceRepository.findByStatusInAndDueDateBefore(
                EnumSet.of(InvoiceStatus.ISSUED, InvoiceStatus.OVERDUE), today);

        DunningRun result = new DunningRun();
        for (Invoice invoice : candidates) {
            if (invoice.outstandingAmount() == 0) {
                continue;
            }
            if (invoice.getStatus() == InvoiceStatus.ISSUED) {
                invoice.setStatus(InvoiceStatus.OVERDUE);
                eventProducer.invoiceOverdue(invoice);
                result.markedOverdue++;
            }

            long daysOverdue = ChronoUnit.DAYS.between(invoice.getDueDate(), today);
            boolean withinGracePeriod = daysOverdue <= gracePeriodDays;
            if (withinGracePeriod && invoice.getLateFeeAmount() == 0) {
                applyLateFee(invoice);
                result.lateFeesApplied++;
            }

            if (reminderDue(invoice, now)) {
                reminderSender.sendOverdueReminder(invoice, daysOverdue);
                invoice.setReminderCount(invoice.getReminderCount() + 1);
                invoice.setLastReminderAt(now);
                result.remindersSent++;
            }
            invoiceRepository.save(invoice);
        }
        log.info("dunning run: {} candidates, {} overdue, {} late fees, {} reminders",
                candidates.size(), result.markedOverdue, result.lateFeesApplied, result.remindersSent);
        return result;
    }

    double lateFeeFor(Invoice invoice) {
        double base = invoice.getTotalAmount() - invoice.getLateFeeAmount();
        return MoneyUtil.roundHalfUp(Math.min(base * lateFeeRate, lateFeeCap));
    }

    private void applyLateFee(Invoice invoice) {
        double fee = lateFeeFor(invoice);
        if (fee <= 0) {
            return;
        }
        invoice.addLine(new InvoiceLine(LATE_FEE_KIND, "Late payment fee", 1, fee));
        invoice.setLateFeeAmount(fee);
        invoice.setTotalAmount(MoneyUtil.roundHalfUp(invoice.getTotalAmount() + fee));
        log.info("late fee {} posted on invoice {}", fee, invoice.getId());
    }

    private boolean reminderDue(Invoice invoice, Instant now) {
        if (invoice.getReminderCount() >= maxReminders) {
            return false;
        }
        Instant last = invoice.getLastReminderAt();
        return last == null || !last.plus(reminderInterval).isAfter(now);
    }

    /** Counters for one pass; returned to the admin endpoint and logged. */
    public static class DunningRun {
        int markedOverdue;
        int lateFeesApplied;
        int remindersSent;

        public int getMarkedOverdue() { return markedOverdue; }
        public int getLateFeesApplied() { return lateFeesApplied; }
        public int getRemindersSent() { return remindersSent; }
    }
}
