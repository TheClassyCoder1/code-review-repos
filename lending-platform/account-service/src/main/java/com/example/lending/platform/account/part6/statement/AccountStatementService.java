package com.example.lending.platform.account.part6.statement;

import com.example.lending.platform.account.entity.Account;
import com.example.lending.platform.account.entity.Ledger;
import com.example.lending.platform.account.part6.money.AccountAmounts;
import com.example.lending.platform.account.repository.AccountRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Builds an account statement from the ledger. The opening balance is derived by backing the
 * ledger entries out of the current balance, so the statement always reconciles to it.
 */
@Service
public class AccountStatementService {

    private final AccountRepository accountRepository;
    private final AccountLedgerRepository ledgerRepository;

    public AccountStatementService(AccountRepository accountRepository, AccountLedgerRepository ledgerRepository) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
    }

    public AccountStatementDto statement(Long accountId) {
        Account account = accountRepository.findById(accountId).orElse(null);
        if (account == null) {
            return null;
        }
        List<Ledger> ledger = ledgerRepository.findByAccountIdOrderByIdAsc(accountId);

        double credits = 0.0;
        double debits = 0.0;
        List<Double> entries = new ArrayList<>(ledger.size());
        for (Ledger line : ledger) {
            double entry = roundToCents(line.getEntry());
            entries.add(entry);
            if (entry >= 0) {
                credits += entry;
            } else {
                debits += -entry;
            }
        }

        AccountStatementDto dto = new AccountStatementDto();
        dto.setAccountId(accountId);
        dto.setClosingBalance(AccountAmounts.forDisplay(account.getBalance()));
        dto.setOpeningBalance(AccountAmounts.forDisplay(account.getBalance() - credits + debits));
        dto.setTotalCredits(roundToCents(credits));
        dto.setTotalDebits(roundToCents(debits));
        dto.setEntries(entries);
        return dto;
    }

    /** Rounds to whole cents, half-up. */
    private static double roundToCents(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }
}
