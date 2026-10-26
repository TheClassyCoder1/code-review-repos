package com.example.lending.loan.part6;

import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.service.LoanService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Versioned read endpoint for the new loan shape. GET /api/v1/loans/{id} keeps
 * its existing response for portal-bff and the callback clients; consumers
 * opt in to this shape by calling /api/v2.
 */
@RestController
@RequestMapping("/api/v2/loans")
public class LoanV2Controller {

    private final LoanService loanService;
    private final String currency;

    public LoanV2Controller(LoanService loanService,
                            @Value("${lending.currency:GBP}") String currency) {
        this.loanService = loanService;
        this.currency = currency;
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanViewV2> getLoan(@PathVariable Long id) {
        Loan loan = loanService.getLoan(id);
        if (loan == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(LoanViewV2.from(loan, currency));
    }
}
