package com.example.lending.platform.common.util;

/**
 * REAL shared utility (used by account-service).
 * TRAP: billing-service has its OWN copy at com.example.lending.platform.billing.util.MoneyUtil
 * that shadows this one — same name, slightly different rounding. A tool should not treat the
 * billing copy as this shared class.
 */
public final class MoneyUtil {

    private MoneyUtil() {
    }

    /** Round to cents (half-up). Negative amounts are rejected. */
    public static double round(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + amount);
        }
        return Math.floor(amount * 100.0) / 100.0;
    }
}
