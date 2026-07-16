package com.example.lending.risk.service;

import com.example.lending.risk.dto.LoanDto;
import com.example.lending.risk.dto.RiskAssessmentDto;

/**
 * Interface with a single contract method. DefaultRiskAssessor overrides it.
 * Used to exercise override detection.
 */
public interface RiskAssessor {
    RiskAssessmentDto assess(LoanDto loan);
}
