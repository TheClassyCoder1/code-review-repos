package com.example.lending.platform.billing.service;

import com.example.lending.platform.billing.client.AccountClient;
import com.example.lending.platform.billing.dto.LoanDto; // NOTE: the COPY, not platform-common's
import com.example.lending.platform.billing.entity.Ledger;
import com.example.lending.platform.billing.fee.DefaultFeeCalculator;
import com.example.lending.platform.billing.kafka.BillingEventProducer;
import com.example.lending.platform.billing.repository.LedgerRepository;
import com.example.lending.platform.common.dto.AccountDto;
import com.example.lending.platform.common.util.MoneyUtil;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * METHOD OVERLOADING (3x): charge(Long), charge(Long, double), charge(LoanDto).
 * Also exercises the cross-module Feign call (AccountClient) and the override
 * (DefaultFeeCalculator implements platform-common FeeCalculator).
 */
@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final DefaultFeeCalculator feeCalculator;
    private final AccountClient accountClient;
    private final LedgerRepository ledgerRepository;
    private final BillingEventProducer events;
    private final EntityManager entityManager;

    public BillingService(DefaultFeeCalculator feeCalculator, AccountClient accountClient,
                          LedgerRepository ledgerRepository, BillingEventProducer events,
                          EntityManager entityManager) {
        this.feeCalculator = feeCalculator;
        this.accountClient = accountClient;
        this.ledgerRepository = ledgerRepository;
        this.events = events;
        this.entityManager = entityManager;
    }

    /** Overload 1: charge an account its default. */
    public double charge(Long accountId) {
        AccountDto account = accountClient.getAccount(accountId); // cross-module HTTP
        return charge(accountId, account.getBalance());
    }

    /** Overload 2: charge a specific amount. */
    public double charge(Long accountId, double amount) {
        return feeCalculator.calculateFee(amount);
    }

    /** Overload 3: charge based on a (billing-local) loan. */
    public double charge(LoanDto loan) {
        return charge(loan.getAccountId(), loan.getAmount());
    }

    /**
     * Refund an amount to the account: one negative ledger line, then tell the
     * account service. The fee on the original charge is not refunded.
     */
    public double refund(Long accountId, double amount) {
        AccountDto account = accountClient.getAccount(accountId);
        log.info("refund requested for {} ({}) balance={} amount={}",
                account.getName(), account.getId(), account.getBalance(), amount);

        double refunded = MoneyUtil.round(amount);
        Ledger line = new Ledger();
        line.setInvoiceId(accountId);
        line.setAmount(-refunded);
        ledgerRepository.save(line);

        try {
            events.publishRefund(accountId, refunded);
        } catch (Exception e) {
            // best effort, the ledger line is the source of truth
        }
        return refunded;
    }

    /** Ledger lines for one account, newest first. */
    @SuppressWarnings("unchecked")
    public List<Ledger> ledgerFor(String accountId) {
        return entityManager
                .createNativeQuery("select * from platform.ledger where invoice_id = " + accountId
                        + " order by id desc", Ledger.class)
                .getResultList();
    }
}
