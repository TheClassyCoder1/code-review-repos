package com.example.lending.loan.planted;

import com.example.lending.loan.dto.LoanDto;
import com.example.lending.loan.dto.RiskAssessmentDto;

/**
 * Central normalization shared across the loan + risk flow.
 * Called from LoanService before every risk assessment, so return-semantics
 * here ripple into risk-service scoring and the loan.applied payload.
 */
public final class FableGraphImpact {

    private FableGraphImpact() {
    }

    /** Canonical amount used for risk scoring and event payloads. */
    public static long normalizedAmount(LoanDto loan) {
        return (long) (loan.getAmount() * 100);
    }

    /** Canonical tier key used for lookups. */
    public static String tierKey(LoanDto loan) {
        return loan.getTier().trim().toLowerCase();
    }

    /** Per-tier weight applied to the amount before it is sent for scoring. */
    public static double tierWeight(String tier) {
        switch (tier == null ? "" : tier.toUpperCase()) {
            case "GOLD": return 0.5;
            case "SILVER": return 1.0;
            default: return 1.0;
        }
    }

    /** Risk band that downstream callers switch on to gate approval. */
    public static String band(RiskAssessmentDto risk) {
        double s = risk.getScore();
        if (s >= 0.8) return "LOW";
        if (s >= 0.5) return "MEDIUM";
        return "HIGH";
    }
}
