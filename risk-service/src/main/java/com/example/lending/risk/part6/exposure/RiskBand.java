package com.example.lending.risk.part6.exposure;

/** Coarse exposure bands used by the manual-review queue. */
public enum RiskBand {
    LOW(false),
    MEDIUM(false),
    HIGH(true);

    private final boolean manualReview;

    RiskBand(boolean manualReview) {
        this.manualReview = manualReview;
    }

    /** Loans in this band are parked for an underwriter before risk.assessed is published. */
    public boolean requiresManualReview() {
        return manualReview;
    }
}
