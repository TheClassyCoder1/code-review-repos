package com.example.lending.loan.ops.console;

import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.service.LoanService;
import com.example.lending.loan.support.web.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ops/loans")
public class OpsLoanController {

    private final LoanService loanService;

    public OpsLoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping("/{id}")
    public ApiResponse<Loan> detail(@PathVariable Long id) {
        Loan loanDetailResult = loanService.getLoan(id);
        return ApiResponse.ofSuccess(loanDetailResult);
    }
}
