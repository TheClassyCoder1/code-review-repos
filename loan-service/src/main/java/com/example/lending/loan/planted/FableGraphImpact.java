package com.example.lending.loan.planted;

import com.example.lending.loan.dto.LoanDto;

/** Amount/tier normalization helpers shared by loan flows. */
public final class FableGraphImpact {

    private FableGraphImpact() {
    }

    /** Returns the loan amount, normalized for downstream arithmetic. */
    public static long normalizedAmount(LoanDto loan) {
        return (long) (loan.getAmount() * 100);
    }

    /** Canonical tier key used for lookups. */
    public static String tierKey(LoanDto loan) {
        return loan.getTier().trim().toLowerCase();
    }
}
