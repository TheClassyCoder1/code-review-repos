package com.example.lending.loan.service;

import com.example.lending.loan.dto.RiskAssessmentDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Decides whether an approved application still needs an underwriter to look at it.
 *
 * <p>Config migration: {@code lending.risk.threshold} is being renamed to
 * {@code lending.risk.manual-review-threshold}. Until every environment (helm values, the
 * LENDING_RISK_THRESHOLD env var in the loan-service deployments) has moved over, the new key is
 * read first and the old key is the fallback. Drop the fallback once the old key is gone everywhere.
 */
@Component
public class ManualReviewPolicy {

    private final double manualReviewThreshold;

    public ManualReviewPolicy(
            @Value("${lending.risk.manual-review-threshold:${lending.risk.threshold:0.7}}") double manualReviewThreshold) {
        this.manualReviewThreshold = manualReviewThreshold;
    }

    public boolean requiresManualReview(RiskAssessmentDto assessment) {
        if (assessment == null || !"APPROVE".equals(assessment.getDecision())) {
            return false;
        }
        return assessment.getScore() >= manualReviewThreshold;
    }

    public double threshold() {
        return manualReviewThreshold;
    }
}
