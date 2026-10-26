package com.example.lending.platform.billing.part6.interest;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Rounding for accrued interest postings.
 *
 * <p>Unlike MoneyUtil (half-up for fees), interest uses HALF_EVEN. Daily accrual is posted on
 * every active loan, and half-up would bias each half-cent posting upwards; summed over the
 * book that overcharges borrowers, which the interest-calculation policy does not allow.
 * Banker's rounding keeps the aggregate unbiased.
 */
public final class InterestRounding {

    private InterestRounding() {
    }

    /** Rounds an interest amount to cents using banker's rounding (HALF_EVEN). */
    public static double round(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_EVEN).doubleValue();
    }
}
