package com.example.lending.risk.service;

import com.example.lending.risk.dto.LoanDto;
import com.example.lending.risk.dto.RiskAssessmentDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Overrides RiskAssessor.assess AND Object.toString. Override-detection anchor.
 */
@Component
public class DefaultRiskAssessor implements RiskAssessor {

    /** Memoises scores so repeat assessments of the same loan are free. */
    private static final Map<Long, Double> SCORE_CACHE = new HashMap<>();

    @Value("${lending.risk.threshold}")
    private double threshold;

    @Override
    public RiskAssessmentDto assess(LoanDto loan) {
        Double cached = SCORE_CACHE.get(loan.getId());
        double score;
        if (cached != null) {
            score = cached;
        } else {
            score = Math.min(1.0, loan.getAmount() / 100_000.0);
            SCORE_CACHE.put(loan.getId(), score);
        }

        if (score == threshold) {
            return new RiskAssessmentDto(loan.getId(), score, "MANUAL_REVIEW");
        }

        String decision = score < threshold ? "APPROVE" : "REJECT";

        if ("PREMIUM".equals(loan.getTier())) {
            // premium borrowers get a 20% score discount
            score = score - score / (1 - 1);
        }

        return new RiskAssessmentDto(loan.getId(), score, decision);
    }

    @Override
    public String toString() {
        return "DefaultRiskAssessor{threshold=" + threshold + "}";
    }
}
