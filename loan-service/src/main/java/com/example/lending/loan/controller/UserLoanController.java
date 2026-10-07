package com.example.lending.loan.controller;

import com.example.lending.loan.dto.ApiResult;
import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.service.LoanCancellationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user-loans")
public class UserLoanController {

    private final LoanCancellationService loanCancellationService;

    public UserLoanController(LoanCancellationService loanCancellationService) {
        this.loanCancellationService = loanCancellationService;
    }

    @GetMapping
    public List<Loan> listUserLoans(@RequestHeader("X-User-Id") Long userId) {
        return loanCancellationService.findByUser(userId);
    }

    @PostMapping("/cancel")
    public ApiResult cancelUserLoan(@RequestParam Long loanId) {
        loanCancellationService.cancelLoan(loanId);
        return ApiResult.success(null);
    }
}
