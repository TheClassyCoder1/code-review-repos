package com.example.lending.loan.controller;

import com.example.lending.loan.dto.LoanApplicationRequest;
import com.example.lending.loan.dto.RiskAssessmentDto;
import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.service.LoanService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REAL cross-repo targets:
 *  - POST /api/v1/loans      <- portal-bff LoanClient (axios)
 *  - GET  /api/v1/loans/{id} <- risk-service callback (RestTemplate) AND loan LoanCallbackClient
 */
@RestController
@RequestMapping("/api/v1/loans")
@CrossOrigin(origins = "*", allowCredentials = "true")
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

    /** Ops console search. */
    @GetMapping("/search")
    public List<Loan> search(@RequestParam String status,
                             @RequestParam(defaultValue = "id") String orderBy) {
        return loanService.searchByStatus(status, orderBy);
    }

    @GetMapping("/export")
    public List<RiskAssessmentDto> export() {
        return loanService.exportAll();
    }

    @GetMapping("/exposure")
    public String exposure() {
        return "total=" + loanService.totalExposure();
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        loanService.deleteLoan(id);
        return "deleted " + id;
    }

    @ExceptionHandler(Exception.class)
    public String handle(Exception e) {
        StringBuilder sb = new StringBuilder(e.toString());
        for (StackTraceElement el : e.getStackTrace()) {
            sb.append("\n\tat ").append(el);
        }
        return sb.toString();
    }
}
