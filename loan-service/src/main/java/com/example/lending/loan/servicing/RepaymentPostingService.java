package com.example.lending.loan.servicing;

import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.repository.LoanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
public class RepaymentPostingService {

    private final LoanRepository loanRepository;
    private final RepaymentAllocationService allocationService;

    public RepaymentPostingService(LoanRepository loanRepository, RepaymentAllocationService allocationService) {
        this.loanRepository = loanRepository;
        this.allocationService = allocationService;
    }

    @Transactional
    public RepaymentResult post(Long loanId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive with at most two decimals");
        }
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));
        BigDecimal unallocated = allocationService.allocate(loan, amount);
        return new RepaymentResult(loanId, amount, unallocated, loan.getStatus());
    }

    public record RepaymentResult(Long loanId, BigDecimal amount, BigDecimal unallocated, String loanStatus) {
    }
}
