package com.acme.claims.model;

public enum ClaimStatus {
    SUBMITTED("Submitted"),
    UNDER_REVIEW("Under review"),
    AWAITING_DOCUMENTS("Awaiting documents"),
    APPROVED("Approved"),
    DENIED("Denied"),
    PAID("Paid"),
    CLOSED("Closed");

    private final String label;

    ClaimStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
