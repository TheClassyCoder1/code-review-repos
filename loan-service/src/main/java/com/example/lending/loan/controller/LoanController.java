package com.example.lending.loan.controller;

import com.example.lending.loan.dto.LoanApplicationRequest;
import com.example.lending.loan.dto.RiskAssessmentDto;
import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.service.LoanService;
import org.springframework.web.bind.annotation.*;

/**
 * REAL cross-repo targets:
 *  - POST /api/v1/loans      <- portal-bff LoanClient (axios)
 *  - GET  /api/v1/loans/{id} <- risk-service callback (RestTemplate) AND loan LoanCallbackClient
 */
@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    public RiskAssessmentDto apply(@RequestBody LoanApplicationRequest request) {
        return loanService.applyLoan(request);
    }

    @GetMapping("/{id}")
    public Loan getLoan(@PathVariable Long id) {
        return loanService.getLoan(id);
    }
}
