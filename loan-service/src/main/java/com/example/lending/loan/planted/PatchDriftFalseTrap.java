package com.example.lending.loan.planted;

/**
 * FALSE trap: shares field shape with the shared LoanDto (id/amount/tier) but is an
 * INDEPENDENT internal analytics row that was never meant to track that contract.
 * A drift tool should NOT report this as a stale copy just because the fields line up.
 */
public class PatchDriftFalseTrap {

    public Long id;
    public double amount;
    public String tier;

    /** Bucketing for an internal histogram — no relation to risk THRESHOLD. */
    public String bucket() {
        if (amount < 10_000) return "small";
        if (amount < 100_000) return "medium";
        return "large";
    }
}
