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

    /** Close an account and release its holds. Called by the offboarding job. */
    public String closeAccount(String accountId, java.util.Map<String, String> holds) {
        String hold = holds.get(accountId);
        return hold.substring(0, 8);          // unchecked: null hold, and shorter than 8 chars
    }
}
