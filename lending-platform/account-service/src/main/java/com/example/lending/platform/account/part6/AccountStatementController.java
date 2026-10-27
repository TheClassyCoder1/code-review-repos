package com.example.lending.platform.account.part6;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Statement balance block for an account period. Called by the statement
 * renderer with the period's ledger entries.
 */
@RestController
@RequestMapping("/api/v1/accounts/{id}/statement-summary")
public class AccountStatementController {

    private final AccountStatementService statementService;

    public AccountStatementController(AccountStatementService statementService) {
        this.statementService = statementService;
    }

    @PostMapping("/{period}")
    public Map<String, Object> summarise(@PathVariable("id") Long id,
                                         @PathVariable("period") String period,
                                         @RequestBody List<Double> entries) {
        return statementService.summarise(id, period, entries);
    }
}
