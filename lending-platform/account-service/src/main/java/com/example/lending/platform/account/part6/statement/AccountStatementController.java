package com.example.lending.platform.account.part6.statement;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** GET /api/v1/accounts/{id}/statement — ledger statement for one account. */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountStatementController {

    private final AccountStatementService statementService;

    public AccountStatementController(AccountStatementService statementService) {
        this.statementService = statementService;
    }

    @GetMapping("/{id}/statement")
    public ResponseEntity<AccountStatementDto> statement(@PathVariable Long id) {
        AccountStatementDto dto = statementService.statement(id);
        return dto == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
    }
}
