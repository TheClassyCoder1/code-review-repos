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

    static final String MODEL_VERSION = "linear-amount-v1";

    // Migration: the threshold moves from lending.risk.threshold to risk.scoring.approve-threshold.
    // Until every environment's config has been moved, the new key wins and the old one is the
    // fallback, so deployments that only set the old key keep their configured value.
    @Value("${risk.scoring.approve-threshold:${lending.risk.threshold}}")
    private double threshold;

    @Override
    public RiskAssessmentDto assess(LoanDto loan) {
        // A non-positive amount scores as zero risk and would be auto-approved. Reject it.
        if (loan.getAmount() <= 0) {
            return withModel(new RiskAssessmentDto(loan.getId(), 1.0, "REJECT"));
        }
        double score = Math.min(1.0, loan.getAmount() / 100_000.0);
        String decision = score <= threshold ? "APPROVE" : "REJECT";
        return withModel(new RiskAssessmentDto(loan.getId(), score, decision));
    }

    private static RiskAssessmentDto withModel(RiskAssessmentDto dto) {
        dto.setModelVersion(MODEL_VERSION);
        return dto;
    }

    @Override
    public String toString() {
        return "DefaultRiskAssessor{threshold=" + threshold + "}";
    }
}
