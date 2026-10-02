package com.example.lending.risk.service;

import com.example.lending.risk.dto.LoanDto;
import com.example.lending.risk.dto.RiskAssessmentDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Overrides RiskAssessor.assess AND Object.toString. Override-detection anchor.
 */
@Component
public class DefaultRiskAssessor implements RiskAssessor {

    @Value("${lending.risk.threshold}")
    private double threshold;

    @Override
    public RiskAssessmentDto assess(LoanDto loan) {
        // A non-positive amount scores as zero risk and would be auto-approved. Reject it.
        if (loan.getAmount() <= 0) {
            return new RiskAssessmentDto(loan.getId(), 1.0, "REJECT");
        }
        double score = Math.min(1.0, loan.getAmount() / 100_000.0);
        String decision = score <= threshold ? "APPROVE" : "REJECT";
        return new RiskAssessmentDto(loan.getId(), score, decision);
    }

    @Override
    public String toString() {
        return "DefaultRiskAssessor{threshold=" + threshold + "}";
    }
}
