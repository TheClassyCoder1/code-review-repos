package com.example.lending.platform.billing.service;

import com.example.lending.platform.billing.client.AccountClient;
import com.example.lending.platform.billing.dto.LoanDto; // NOTE: the COPY, not platform-common's
import com.example.lending.platform.billing.fee.DefaultFeeCalculator;
import com.example.lending.platform.common.dto.AccountDto;
import org.springframework.stereotype.Service;

/**
 * METHOD OVERLOADING (3x): charge(Long), charge(Long, double), charge(LoanDto).
 * Also exercises the cross-module Feign call (AccountClient) and the override
 * (DefaultFeeCalculator implements platform-common FeeCalculator).
 */
@Service
public class BillingService {

    private static final String ACTIVE = "ACTIVE";

    private final DefaultFeeCalculator feeCalculator;
    private final AccountClient accountClient;

    public BillingService(DefaultFeeCalculator feeCalculator, AccountClient accountClient) {
        this.feeCalculator = feeCalculator;
        this.accountClient = accountClient;
    }

    /**
     * Overload 1: charge an account its default. Returns null when the account is not
     * ACTIVE (frozen or closed accounts accrue no fee), so callers can tell "nothing to
     * charge" apart from a zero fee.
     */
    public Double charge(Long accountId) {
        AccountDto account = accountClient.getAccount(accountId); // cross-module HTTP
        if (account == null) {
            // account-service answers 200 with an empty body for unknown ids
            throw new IllegalArgumentException("account not found: " + accountId);
        }
        if (!ACTIVE.equals(account.getStatus())) {
            return null;
        }
        return charge(accountId, account.getBalance());
    }

    /** Overload 2: charge a specific amount. */
    public double charge(Long accountId, double amount) {
        // NaN fails every comparison, so it slips past the negative check and produces a NaN fee.
        if (Double.isNaN(amount)) {
            throw new IllegalArgumentException("charge amount must be a number for account " + accountId);
        }
        // A negative amount yields a negative fee, which credits the account instead of charging it.
        if (amount < 0) {
            throw new IllegalArgumentException("charge amount must not be negative for account " + accountId);
        }
        return feeCalculator.calculateFee(amount);
    }

    /** Overload 3: charge based on a (billing-local) loan. */
    public double charge(LoanDto loan) {
        // Loans not yet linked to an account would compute a fee that is never posted anywhere.
        if (loan.getAccountId() == null) {
            throw new IllegalArgumentException("loan " + loan.getId() + " has no billing account");
        }
        return charge(loan.getAccountId(), loan.getAmount());
    }
}
