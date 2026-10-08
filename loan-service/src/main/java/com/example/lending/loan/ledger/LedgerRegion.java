package com.example.lending.loan.ledger;

public enum LedgerRegion {
    NORTH("N"),
    SOUTH("S"),
    EAST("E"),
    WEST("W");

    private final String code;

    LedgerRegion(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
