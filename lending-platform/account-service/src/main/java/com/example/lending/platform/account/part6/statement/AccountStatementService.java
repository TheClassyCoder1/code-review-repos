package com.example.lending.platform.account.part6.statement;

import com.example.lending.platform.account.entity.Account;
import com.example.lending.platform.account.entity.Ledger;
import com.example.lending.platform.account.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Builds an account statement from the ledger. The stored account balance is the closing
 * balance; the opening balance is derived by unwinding the ledger entries.
 */
@Service
public class AccountStatementService {

    private static final int INTEREST_PROJECTION_DAYS = 30;

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final BigDecimal annualInterestRate;

    public AccountStatementService(AccountRepository accountRepository,
                                   LedgerEntryRepository ledgerRepository,
                                   @Value("${platform.interest.annual-rate:0.045}") BigDecimal annualInterestRate) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
        this.annualInterestRate = annualInterestRate;
    }

    public Optional<AccountStatement> statementFor(Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("accountId is required");
        }
        return accountRepository.findById(accountId).map(this::build);
    }

    private AccountStatement build(Account account) {
        List<Ledger> entries = ledgerRepository.findByAccountIdOrderByIdAsc(account.getId());

        double credits = 0;
        double debits = 0;
        for (Ledger entry : entries) {
            if (entry.getEntry() >= 0) {
                credits += entry.getEntry();
            } else {
                debits += -entry.getEntry();
            }
        }
        double closing = roundCents(account.getBalance());
        double opening = roundCents(closing - credits + debits);

        List<AccountStatement.Line> lines = new ArrayList<>(entries.size());
        double running = opening;
        for (Ledger entry : entries) {
            running = roundCents(running + entry.getEntry());
            lines.add(new AccountStatement.Line(
                    entry.getId(),
                    Cents.of(Math.abs(entry.getEntry())),
                    entry.getEntry() >= 0 ? "CREDIT" : "DEBIT",
                    Cents.of(running)));
        }

        BigDecimal projectedInterest = InterestAccrual.accrue(
                Cents.of(closing), annualInterestRate, INTEREST_PROJECTION_DAYS);

        return new AccountStatement(
                account.getId(),
                account.getStatus(),
                Cents.of(opening),
                Cents.of(credits),
                Cents.of(debits),
                Cents.of(closing),
                projectedInterest,
                lines);
    }

    /** Round to cents, half-up. */
    private static double roundCents(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }
}
