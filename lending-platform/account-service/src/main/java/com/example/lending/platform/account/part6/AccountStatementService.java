package com.example.lending.platform.account.part6;

import com.example.lending.platform.account.entity.Account;
import com.example.lending.platform.account.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Builds the month-end statement summary for an account: opening balance,
 * posted movements, interest accrued on the daily balance and the closing
 * balance. Movements are supplied by the caller (ledger export) as signed
 * amounts keyed by posting day of month.
 */
@Service
public class AccountStatementService {

    public record Summary(Long accountId, double opening, double credits, double debits,
                          double interest, double closing, List<Double> dailyBalances) {
    }

    private final AccountRepository accountRepository;

    public AccountStatementService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Summary summarise(Long accountId, Map<Integer, List<Double>> movementsByDay,
                             int daysInMonth, double annualRate) {
        if (daysInMonth < 28 || daysInMonth > 31) {
            throw new IllegalArgumentException("daysInMonth must be between 28 and 31");
        }
        if (!Double.isFinite(annualRate) || annualRate < 0) {
            throw new IllegalArgumentException("annualRate must be a non-negative number");
        }
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("account not found: " + accountId));

        double opening = account.getBalance();
        double running = opening;
        double credits = 0;
        double debits = 0;
        double interest = 0;
        List<Double> daily = new ArrayList<>(daysInMonth);
        double dailyRate = annualRate / 365.0;

        for (int day = 1; day <= daysInMonth; day++) {
            for (double movement : movementsByDay.getOrDefault(day, Collections.emptyList())) {
                if (movement >= 0) {
                    credits += movement;
                } else {
                    debits += -movement;
                }
                running += movement;
            }
            if (running > 0) {
                interest += running * dailyRate;
            }
            daily.add(roundToCents(running));
        }

        double closing = running + interest;
        return new Summary(accountId, roundToCents(opening), roundToCents(credits), roundToCents(debits),
                roundToCents(interest), roundToCents(closing), List.copyOf(daily));
    }

    private static double roundToCents(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }
}
