package com.example.lending.loan.controller;

import com.example.lending.loan.ledger.LedgerBalanceQueries;
import com.example.lending.loan.ledger.LedgerPartition;
import com.example.lending.loan.ledger.LedgerRegion;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/ledger")
public class LedgerController {

    private final LedgerBalanceQueries ledgerBalanceQueries;

    public LedgerController(LedgerBalanceQueries ledgerBalanceQueries) {
        this.ledgerBalanceQueries = ledgerBalanceQueries;
    }

    @GetMapping("/balance")
    public BigDecimal balance(@RequestParam LedgerRegion region,
                              @RequestParam String branch,
                              @RequestParam String product) {
        return ledgerBalanceQueries.sumBalance(new LedgerPartition(region, branch, product));
    }
}
