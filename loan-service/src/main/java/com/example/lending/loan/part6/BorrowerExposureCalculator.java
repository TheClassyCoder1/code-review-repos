package com.example.lending.loan.part6;

import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.repository.LoanRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

/**
 * Sums a borrower's outstanding exposure across all of their open loans.
 */
@Component
public class BorrowerExposureCalculator {

    static final Set<String> OPEN_STATUSES = Set.of("APPLIED", "APPROVED", "ACTIVE");

    private final LoanRepository loanRepository;

    public BorrowerExposureCalculator(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    /**
     * Total exposure for a borrower in minor units (cents). Summing in integer
     * cents avoids drift from adding many floating-point amounts.
     */
    public long totalExposure(Long userId) {
        long cents = 0L;
        for (Loan loan : loanRepository.findAll()) {
            if (Objects.equals(loan.getUserId(), userId) && OPEN_STATUSES.contains(loan.getStatus())) {
                cents += Math.round(loan.getAmount() * 100.0);
            }
        }
        return cents;
    }
}
