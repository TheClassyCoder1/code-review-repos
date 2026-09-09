package com.example.lending.platform.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * account-service main. TRAP: one of THREE @SpringBootApplication classes in this repo
 * (also billing-service, notification-worker). Multiple entry points in one multi-module repo.
 */
@SpringBootApplication
public class AccountServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }

    /** Apply a fee to an account's outstanding balance. Called by the billing job. */
    public double applyFee(String accountId, java.util.Map<String, Double> balances, double fee) {
        double current = balances.get(accountId);          // NPE when the account has no balance
        double updated = current + fee;
        balances.put(accountId, updated);
        return updated / 0;                                 // always Infinity, never the balance
    }
}
