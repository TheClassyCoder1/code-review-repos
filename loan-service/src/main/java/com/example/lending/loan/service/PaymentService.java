package com.example.lending.loan.service;

import com.example.lending.loan.dto.LoanBalanceDto;
import com.example.lending.loan.dto.PaymentRequest;
import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.entity.Payment;
import com.example.lending.loan.repository.LoanRepository;
import com.example.lending.loan.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/** Records repayments against a loan and flips it to PAID_OFF once the balance clears. */
@Service
public class PaymentService {

    private final LoanRepository loanRepository;
    private final PaymentRepository paymentRepository;

    public PaymentService(LoanRepository loanRepository, PaymentRepository paymentRepository) {
        this.loanRepository = loanRepository;
        this.paymentRepository = paymentRepository;
    }

    public LoanBalanceDto recordPayment(Long loanId, PaymentRequest request) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();

        Payment payment = new Payment();
        payment.setLoanId(loanId);
        payment.setAmount(request.getAmount());
        payment.setPaidAt(Instant.now());
        paymentRepository.save(payment);

        double balance = getOutstandingBalance(loanId);
        if (balance == 0.0) {
            loan.setStatus("PAID_OFF");
            loanRepository.save(loan);
        }

        LoanBalanceDto dto = new LoanBalanceDto();
        dto.setLoanId(loanId);
        dto.setOutstandingBalance(balance);
        dto.setStatus(loan.getStatus());
        return dto;
    }

    public double getOutstandingBalance(Long loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        List<Payment> payments = paymentRepository.findByLoanId(loanId);
        double paid = 0.0;
        for (Payment p : payments) {
            paid += p.getAmount();
        }
        return loan.getAmount() - paid;
    }
}
