package com.example.lending.platform.billing.invoice;

import com.example.lending.platform.billing.service.BillingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Month-end run: computes the maintenance fee for each billable account and returns
 * the invoice lines for the ledger poster. One failing account must not abort the
 * rest of the run, so failures are collected and reported alongside the lines.
 */
@Component
public class MonthlyInvoiceJob {

    private static final Logger log = LoggerFactory.getLogger(MonthlyInvoiceJob.class);

    private final BillingService billingService;

    public MonthlyInvoiceJob(BillingService billingService) {
        this.billingService = billingService;
    }

    public RunResult run(YearMonth period, List<Long> accountIds) {
        List<InvoiceLine> lines = new ArrayList<>();
        List<Long> failed = new ArrayList<>();
        for (Long accountId : accountIds) {
            try {
                double fee = billingService.charge(accountId);
                if (fee > 0) {
                    lines.add(new InvoiceLine(accountId, period, fee));
                }
            } catch (IllegalArgumentException ex) {
                log.warn("invoice run {} skipped accountId={}: {}", period, accountId, ex.getMessage());
                failed.add(accountId);
            }
        }
        log.info("invoice run {} produced {} lines, {} failures", period, lines.size(), failed.size());
        return new RunResult(lines, failed);
    }

    public record InvoiceLine(Long accountId, YearMonth period, double feeAmount) {
    }

    public record RunResult(List<InvoiceLine> lines, List<Long> failedAccountIds) {
    }
}
