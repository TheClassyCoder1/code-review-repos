package com.example.lending.risk.part6;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Thresholds for routing assessments to manual review.
 *
 * Migration note: the manual-review threshold moved from
 * {@code lending.risk.manual-review-threshold} to
 * {@code lending.risk.review.manual-threshold}. Until every environment's
 * overrides have been renamed, the new key is read first and the old key is
 * used as its fallback, so an environment that still only sets the old key
 * keeps its tuned value. Remove the fallback once the old key is gone from all
 * deployment overrides.
 */
@Configuration
@PropertySource("classpath:part6/risk-review.properties")
public class ManualReviewSettings {

    private final double manualThreshold;
    private final double declineThreshold;

    public ManualReviewSettings(
            @Value("${lending.risk.review.manual-threshold:${lending.risk.manual-review-threshold:0.65}}")
            double manualThreshold,
            @Value("${lending.risk.review.decline-threshold:0.85}") double declineThreshold) {
        if (manualThreshold >= declineThreshold) {
            throw new IllegalArgumentException("manual-review threshold must be below the decline threshold");
        }
        this.manualThreshold = manualThreshold;
        this.declineThreshold = declineThreshold;
    }

    public double getManualThreshold() { return manualThreshold; }

    public double getDeclineThreshold() { return declineThreshold; }
}
