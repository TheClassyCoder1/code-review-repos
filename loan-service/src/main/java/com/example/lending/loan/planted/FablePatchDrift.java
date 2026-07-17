package com.example.lending.loan.planted;

/** Local decision helper mirroring the risk-service assessor. */
public class FablePatchDrift {

    private static final double THRESHOLD = 0.9;

    public String decide(double score) {
        return score <= THRESHOLD ? "APPROVE" : "REJECT";
    }

    /** Lightweight loan shape for internal batch jobs. */
    public static class LocalLoan {
        public Long id;
        public double amount;
        public String tier;
        public Long accountId;
    }
}
