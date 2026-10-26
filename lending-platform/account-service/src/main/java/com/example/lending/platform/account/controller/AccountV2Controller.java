package com.example.lending.platform.account.controller;

import com.example.lending.platform.account.dto.AccountSummaryV2;
import com.example.lending.platform.account.service.AccountService;
import com.example.lending.platform.account.service.BalanceFormatter;
import com.example.lending.platform.common.dto.AccountDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * v2 account read: exact-decimal balance string, explicit currency, 404 for unknown ids.
 * /api/v1/accounts/{id} (AccountController) is left exactly as it was for its Feign callers.
 */
@RestController
@RequestMapping("/api/v2/accounts")
public class AccountV2Controller {

    static final String CURRENCY = "EUR";

    private final AccountService accountService;
    private final BalanceFormatter balanceFormatter;

    public AccountV2Controller(AccountService accountService, BalanceFormatter balanceFormatter) {
        this.accountService = accountService;
        this.balanceFormatter = balanceFormatter;
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountSummaryV2> get(@PathVariable Long id) {
        AccountDto account = accountService.get(id);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new AccountSummaryV2(
                account.getId(),
                account.getStatus(),
                balanceFormatter.format(account.getBalance()),
                CURRENCY,
                "ACTIVE".equals(account.getStatus())));
    }
}
