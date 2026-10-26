package com.example.lending.risk.service;

import com.example.lending.risk.config.RiskBandProperties;
import com.example.lending.risk.dto.LoanDto;
import com.example.lending.risk.dto.RiskAssessmentV2Dto;
import com.example.lending.risk.hook.RiskDecisionHook;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/** Banded decision (APPROVE / REVIEW / REJECT) behind the /api/v2/risk endpoints. */
@Service
public class RiskBandService {

    private final ScoreCalculator scoreCalculator;
    private final RiskBandProperties bands;
    private final List<RiskDecisionHook> hooks;

    public RiskBandService(ScoreCalculator scoreCalculator, RiskBandProperties bands, List<RiskDecisionHook> hooks) {
        this.scoreCalculator = scoreCalculator;
        this.bands = bands;
        this.hooks = hooks;
    }

    public RiskAssessmentV2Dto assess(LoanDto loan) {
        if (loan.getId() == null) {
            throw new IllegalArgumentException("loan id is required for risk assessment");
        }
        hooks.forEach(hook -> hook.beforeDecision(loan));

        double score = scoreCalculator.score(loan);
        String band;
        String decision;
        if (score <= bands.getMaxScore()) {
            band = "LOW";
            decision = "APPROVE";
        } else if (score <= bands.getReviewMaxScore()) {
            band = "MEDIUM";
            decision = "REVIEW";
        } else {
            band = "HIGH";
            decision = "REJECT";
        }
        RiskAssessmentV2Dto result = new RiskAssessmentV2Dto(loan.getId(), score, band, decision, Instant.now());

        hooks.forEach(hook -> hook.afterDecision(result));
        return result;
    }

    public RiskAssessmentV2Dto assess(Long loanId, String tier) {
        return assess(new LoanDto(loanId, 50_000.0, tier, null));
    }
}
