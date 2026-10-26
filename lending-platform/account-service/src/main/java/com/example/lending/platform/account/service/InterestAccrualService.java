package com.example.lending.platform.account.service;

import com.example.lending.platform.account.entity.Account;
import com.example.lending.platform.account.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Daily interest accrual for savings-style accounts. Uses simple interest on the current
 * balance with an actual/365 day count.
 */
@Service
public class InterestAccrualService {

    static final int DAYS_PER_YEAR = 365;

    private final AccountRepository accountRepository;
    private final double annualRate;

    public InterestAccrualService(AccountRepository accountRepository,
                                  @Value("${platform.interest.annual-rate:0.015}") double annualRate) {
        this.accountRepository = accountRepository;
        this.annualRate = annualRate;
    }

    /** Accrues {@code days} of interest onto the account and returns the interest added. */
    public double accrue(Long accountId, int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("days must be positive");
        }
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("account not found: " + accountId));
        if (!"ACTIVE".equals(account.getStatus()) || account.getBalance() <= 0) {
            return 0;
        }
        double interest = roundToCents(account.getBalance() * annualRate * days / DAYS_PER_YEAR);
        account.setBalance(roundToCents(account.getBalance() + interest));
        accountRepository.save(account);
        return interest;
    }

    private static double roundToCents(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }
}
