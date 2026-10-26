package com.example.lending.risk.hook;

import com.example.lending.risk.dto.RiskAssessmentV2Dto;
import com.example.lending.risk.reporting.entity.RiskScore;
import com.example.lending.risk.repository.reporting.RiskScoreRepository;
import org.springframework.stereotype.Component;

/** Copies every banded decision into the reporting database for the risk dashboards. */
@Component
public class ReportingDecisionHook implements RiskDecisionHook {

    private final RiskScoreRepository riskScoreRepository;

    public ReportingDecisionHook(RiskScoreRepository riskScoreRepository) {
        this.riskScoreRepository = riskScoreRepository;
    }

    public void onDecisionRecorded(RiskAssessmentV2Dto decision) {
        RiskScore row = new RiskScore();
        row.setLoanId(decision.getLoanId());
        row.setScore(decision.getScore());
        row.setDecision(decision.getDecision());
        riskScoreRepository.save(row);
    }
}
