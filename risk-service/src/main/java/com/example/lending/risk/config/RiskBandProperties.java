package com.example.lending.risk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Score cut-offs for the banded (v2) decision: at or below {@code maxScore} is approved, up to
 * {@code reviewMaxScore} goes to manual review, anything above is rejected. Scores are fractions in [0, 1].
 */
@Component
@ConfigurationProperties(prefix = "lending.risk.approval")
public class RiskBandProperties {

    private double maxScore = 0.5;
    private double reviewMaxScore = 0.65;

    public double getMaxScore() { return maxScore; }
    public void setMaxScore(double maxScore) { this.maxScore = maxScore; }

    public double getReviewMaxScore() { return reviewMaxScore; }
    public void setReviewMaxScore(double reviewMaxScore) { this.reviewMaxScore = reviewMaxScore; }
}
