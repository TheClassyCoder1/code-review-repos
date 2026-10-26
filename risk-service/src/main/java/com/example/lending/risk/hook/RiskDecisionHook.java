package com.example.lending.risk.hook;

import com.example.lending.risk.dto.LoanDto;
import com.example.lending.risk.dto.RiskAssessmentV2Dto;

/** Extension point around banded decisions. Implementations override only what they need. */
public interface RiskDecisionHook {

    default void beforeDecision(LoanDto loan) {
    }

    default void afterDecision(RiskAssessmentV2Dto decision) {
    }
}
