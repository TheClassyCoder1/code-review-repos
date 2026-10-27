package com.example.lending.risk.part6.band;

/** Coarse risk bands used by underwriting dashboards. Scores are 0.0 (safe) to 1.0 (risky). */
public enum RiskBand {
    LOW(0.0, 0.25),
    MODERATE(0.25, 0.5),
    ELEVATED(0.5, 0.75),
    HIGH(0.75, 1.0);

    private final double lowerInclusive;
    private final double upperExclusive;

    RiskBand(double lowerInclusive, double upperExclusive) {
        this.lowerInclusive = lowerInclusive;
        this.upperExclusive = upperExclusive;
    }

    public double getLowerInclusive() { return lowerInclusive; }

    public double getUpperExclusive() { return upperExclusive; }
}
