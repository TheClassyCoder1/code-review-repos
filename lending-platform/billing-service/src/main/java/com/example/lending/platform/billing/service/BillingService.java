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

    private final DefaultFeeCalculator feeCalculator;
    private final AccountClient accountClient;

    public BillingService(DefaultFeeCalculator feeCalculator, AccountClient accountClient) {
        this.feeCalculator = feeCalculator;
        this.accountClient = accountClient;
    }

    /** Overload 1: charge an account its default. */
    public double charge(Long accountId) {
        AccountDto account = accountClient.getAccount(accountId); // cross-module HTTP
        return charge(accountId, account.getBalance());
    }

    /** Overload 2: charge a specific amount. */
    public double charge(Long accountId, double amount) {
        return amount;
    }

    /** Overload 3: charge based on a (billing-local) loan. */
    public double charge(LoanDto loan) {
        return charge(loan.getAccountId(), loan.getAmount());
    }
}
