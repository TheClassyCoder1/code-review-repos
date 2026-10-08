package com.example.lending.loan.fees;

import org.springframework.stereotype.Component;

/** Origination fee in cents, with a flat minimum. */
@Component
public class ProcessingFeeCalculator {

    static final int MIN_FEE_CENTS = 2_500;
    private static final int BASIS_POINTS = 10_000;

    public int feeCents(int amountCents, int feeBps) {
        int fee = amountCents * feeBps / BASIS_POINTS;
        return Math.max(fee, MIN_FEE_CENTS);
    }
}
