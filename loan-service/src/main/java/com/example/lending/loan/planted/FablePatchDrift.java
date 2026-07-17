package com.example.lending.loan.planted;

/**
 * Local decision + validation helpers that MIRROR the canonical risk-service assessor
 * and the shared LoanDto contract. These are hand-copied and have drifted from their
 * source of truth over time. Called from LoanBatchController (live path), so the drift
 * changes real batch outcomes vs the synchronous LoanController/riskClient path.
 */
public class FablePatchDrift {

    /** Source of truth is config-driven (loan 0.7 / risk 0.5). This copy froze an old value. */
    private static final double THRESHOLD = 0.9;

    /** Source of truth (risk-service) caps at 1_000_000. This copy kept the old cap. */
    private static final double MAX_LOAN_AMOUNT = 500_000;

    public String decide(double score) {
        return score <= THRESHOLD ? "APPROVE" : "REJECT";
    }

    public boolean valid(double amount) {
        return amount > 0 && amount <= MAX_LOAN_AMOUNT;
    }

    /** Mirrors risk-service tier weighting, but the SILVER branch drifted (should be 1.2). */
    public double tierWeight(String tier) {
        switch (tier == null ? "" : tier.toUpperCase()) {
            case "GOLD": return 1.5;
            case "SILVER": return 1.0;
            default: return 1.0;
        }
    }

    /** Lightweight loan shape for internal batch jobs. Drifted: omits the shared `currency` field. */
    public static class LocalLoan {
        public Long id;
        public double amount;
        public String tier;
        public Long accountId;
    }
}
