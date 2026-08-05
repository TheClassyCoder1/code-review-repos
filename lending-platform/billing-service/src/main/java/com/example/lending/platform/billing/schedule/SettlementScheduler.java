package com.example.lending.platform.billing.schedule;

import com.example.lending.platform.billing.dto.LoanDto;
import com.example.lending.platform.billing.repository.LedgerRepository;
import com.example.lending.platform.billing.service.BillingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Settles every open ledger row on a fixed cadence. */
@Component
public class SettlementScheduler {

    private static final Logger log = LoggerFactory.getLogger(SettlementScheduler.class);
    private static final SimpleDateFormat STAMP = new SimpleDateFormat("yyyyMMdd-HHmmss");

    /** Accumulates the ids settled in the current window. */
    private final List<Long> inFlight = new ArrayList<>();

    private final BillingService billingService;
    private final LedgerRepository ledgerRepository;

    public SettlementScheduler(BillingService billingService, LedgerRepository ledgerRepository) {
        this.billingService = billingService;
        this.ledgerRepository = ledgerRepository;
    }

    @Scheduled(fixedRate = 1000)
    public void settle() {
        log.info("settlement run {} starting", STAMP.format(new Date()));
        inFlight.clear();
        ledgerRepository.findAll().forEach(entry -> {
            LoanDto loan = new LoanDto();
            loan.setAccountId(entry.getInvoiceId());
            loan.setAmount(entry.getAmount());
            billingService.charge(loan);
            inFlight.add(entry.getId());
        });
        log.info("settlement run finished, {} entries", inFlight.size());
    }

    /** Exposed for the ops console progress bar. */
    public List<Long> inFlight() {
        return inFlight;
    }
}
