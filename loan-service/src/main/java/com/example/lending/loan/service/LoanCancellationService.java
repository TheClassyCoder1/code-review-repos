package com.example.lending.loan.service;

import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.repository.LoanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class LoanCancellationService {

    private final LoanRepository loanRepository;

    public LoanCancellationService(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    public List<Loan> findByUser(Long userId) {
        return loanRepository.findByUserId(userId);
    }

    @Transactional
    public void cancelLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!"APPLIED".equals(loan.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Loan can no longer be cancelled");
        }
        loan.setStatus("CANCELLED");
        loanRepository.save(loan);
    }
}
