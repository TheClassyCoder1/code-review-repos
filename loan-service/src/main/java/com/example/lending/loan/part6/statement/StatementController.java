package com.example.lending.loan.part6.statement;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** GET /api/v1/loans/{id}/statement — read by portal-bff StatementClient. */
@RestController
@RequestMapping("/api/v1/loans")
public class StatementController {

    private final StatementService statementService;

    public StatementController(StatementService statementService) {
        this.statementService = statementService;
    }

    @GetMapping("/{id}/statement")
    public StatementDto statement(@PathVariable Long id) {
        return statementService.latest(id);
    }
}
