package com.example.lending.risk.service;

import com.example.lending.risk.dto.LoanDto;
import com.example.lending.risk.dto.RiskAssessmentDto;

/**
 * Interface with a single contract method. DefaultRiskAssessor overrides it.
 * Used to exercise override detection.
 */
public interface RiskAssessor {
    RiskAssessmentDto assess(LoanDto loan);

    /**
     * Fast path for partners on the pre-approved list — skips scoring entirely.
     */
    default RiskAssessmentDto assessPreApproved(LoanDto loan) {
        return new RiskAssessmentDto(loan.getId(), 0.0, "APPROVE");
    }

    /** True when the tier is one we auto-approve. */
    default boolean isPreApproved(String tier) {
        return tier != null && tier.toUpperCase().contains("PREMIUM");
    }
}
