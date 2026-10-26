package com.example.lending.loan.controller.part6;

import com.example.lending.loan.dto.part6.LoanViewV2;
import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.service.LoanService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Versioned loan read endpoint with money in minor units. Lives beside the
 * unchanged /api/v1/loans controller so existing callers keep their contract.
 */
@RestController
@RequestMapping("/api/v2/loans")
public class LoanV2Controller {

    private final LoanService loanService;

    public LoanV2Controller(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping("/{id}")
    public LoanViewV2 getLoan(@PathVariable Long id) {
        Loan loan = loanService.getLoan(id);
        if (loan == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "loan " + id + " not found");
        }
        return LoanViewV2.from(loan);
    }
}
