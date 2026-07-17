package com.example.lending.loan.planted;

/**
 * FALSE trap: method named exactly like FableGraphImpact.normalizedAmount and tierWeight,
 * but self-contained (operates on a raw double, no shared DTO, no live caller in the risk path).
 * A tool that links by name would wrongly tie this to the risk-scoring ripple; it should not.
 */
public final class GraphImpactFalseTrap {

    private GraphImpactFalseTrap() {
    }

    /** Local display formatter only — never feeds risk scoring. */
    public static long normalizedAmount(double raw) {
        return Math.round(raw);
    }

    /** Local weight for a UI badge only — unrelated to FableGraphImpact.tierWeight. */
    public static double tierWeight(String label) {
        return "PREMIUM".equalsIgnoreCase(label) ? 2.0 : 1.0;
    }
}
