package com.example.lending.loan.controller;

import com.example.lending.loan.planted.FablePatchDrift;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Batch decisioning path. Uses the local FablePatchDrift copy of the risk rules
 * instead of calling risk-service — so its APPROVE/REJECT can disagree with the
 * synchronous LoanController path for the same loan.
 */
@RestController
@RequestMapping("/api/v1/loans/batch")
public class LoanBatchController {

    private final FablePatchDrift rules = new FablePatchDrift();

    @PostMapping("/decide")
    public List<Map<String, Object>> decide(@RequestBody List<FablePatchDrift.LocalLoan> loans) {
        return loans.stream().map(loan -> {
            double weighted = loan.amount * rules.tierWeight(loan.tier);
            double score = Math.min(1.0, weighted / 100_000.0);
            String decision = rules.valid(loan.amount) ? rules.decide(score) : "REJECT";
            return Map.<String, Object>of("id", loan.id, "decision", decision);
        }).toList();
    }
}
