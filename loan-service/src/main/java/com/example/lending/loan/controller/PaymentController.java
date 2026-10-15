package com.example.lending.loan.controller;

import com.example.lending.loan.dto.LoanBalanceDto;
import com.example.lending.loan.dto.PaymentRequest;
import com.example.lending.loan.service.PaymentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/loans/{loanId}")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payments")
    public LoanBalanceDto recordPayment(@PathVariable Long loanId, @RequestBody PaymentRequest request) {
        return paymentService.recordPayment(loanId, request);
    }

    @GetMapping("/balance")
    public LoanBalanceDto getBalance(@PathVariable Long loanId) {
        LoanBalanceDto dto = new LoanBalanceDto();
        dto.setLoanId(loanId);
        dto.setOutstandingBalance(paymentService.getOutstandingBalance(loanId));
        return dto;
    }
}
