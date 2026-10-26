package com.example.lending.risk.part6;

import org.springframework.stereotype.Component;

/**
 * Maps a risk score to a review band: AUTO (straight-through), MANUAL
 * (underwriter review) or DECLINE.
 */
@Component
public class RiskBandClassifier {

    public static final String AUTO = "AUTO";
    public static final String MANUAL = "MANUAL";
    public static final String DECLINE = "DECLINE";

    /** Premium-tier loans get a small allowance before they need review. */
    static final double PREMIUM_ALLOWANCE = 0.05;

    private final ManualReviewSettings settings;

    public RiskBandClassifier(ManualReviewSettings settings) {
        this.settings = settings;
    }

    /**
     * Band for a score, taking the loan tier into account. {@code tier} may be
     * null when it is not known to the caller.
     */
    public String bandFor(double score, String tier) {
        if (score >= settings.getDeclineThreshold()) {
            return DECLINE;
        }
        double manual = settings.getManualThreshold();
        if ("PREMIUM".equalsIgnoreCase(tier)) {
            manual = Math.min(manual + PREMIUM_ALLOWANCE, settings.getDeclineThreshold());
        }
        return score >= manual ? MANUAL : AUTO;
    }

    /**
     * Tier-less banding.
     *
     * @deprecated use {@link #bandFor(double, String)}; kept so existing
     * callers keep working while they migrate.
     */
    @Deprecated
    public String classify(double score) {
        return bandFor(score, null);
    }
}
