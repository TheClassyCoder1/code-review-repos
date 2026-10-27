package com.example.lending.platform.account.part6;

import com.example.lending.platform.account.entity.Account;
import com.example.lending.platform.account.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the balance block of a monthly account statement.
 */
@Service
public class AccountStatementService {

    private final AccountRepository accountRepository;

    public AccountStatementService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Summarise a statement period for an account. The opening balance is
     * derived from the current balance minus the period's entries.
     */
    public Map<String, Object> summarise(Long accountId, String period, List<Double> periodEntries) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("account not found: " + accountId));

        double movement = 0.0;
        for (Double entry : periodEntries) {
            if (entry != null) {
                movement += entry;
            }
        }
        StatementBalanceSummary summary = StatementBalanceSummary.of(account.getBalance() - movement, periodEntries);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accountId", accountId);
        body.put("period", period);
        body.put("openingBalance", amount(summary.getOpeningBalance()));
        body.put("totalDebits", amount(summary.getTotalDebits()));
        body.put("totalCredits", amount(summary.getTotalCredits()));
        body.put("closingBalance", amount(summary.getClosingBalance()));
        return body;
    }

    private static BigDecimal amount(double value) {
        return BalanceAmounts.toStatementAmount(value);
    }
}
