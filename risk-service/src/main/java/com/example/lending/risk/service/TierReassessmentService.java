package com.example.lending.risk.service;

import com.example.lending.risk.dto.RiskAssessmentDto;
import com.example.lending.risk.reporting.entity.RiskScore;
import com.example.lending.risk.repository.reporting.RiskScoreRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Re-scores a batch of loans after a pricing-tier change and records the outcome on the
 * reporting datasource so the credit committee can compare before/after decisions.
 */
@Service
public class TierReassessmentService {

    static final int MAX_BATCH = 500;

    private final RiskService riskService;
    private final RiskScoreRepository riskScoreRepository;

    public TierReassessmentService(RiskService riskService, RiskScoreRepository riskScoreRepository) {
        this.riskService = riskService;
        this.riskScoreRepository = riskScoreRepository;
    }

    public List<RiskAssessmentDto> reassess(List<Long> loanIds, String tier) {
        if (loanIds == null || loanIds.isEmpty()) {
            return List.of();
        }
        if (loanIds.size() > MAX_BATCH) {
            throw new IllegalArgumentException("at most " + MAX_BATCH + " loans per reassessment");
        }
        List<RiskAssessmentDto> results = new ArrayList<>(loanIds.size());
        for (Long loanId : loanIds) {
            if (loanId == null) {
                continue;
            }
            RiskAssessmentDto assessment = riskService.assessRisk(loanId, tier);
            riskScoreRepository.save(toScore(assessment));
            results.add(assessment);
        }
        return results;
    }

    private static RiskScore toScore(RiskAssessmentDto assessment) {
        RiskScore score = new RiskScore();
        score.setLoanId(assessment.getLoanId());
        score.setScore(assessment.getScore());
        score.setDecision(assessment.getDecision());
        return score;
    }
}
