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

    /** Round to cents (half-up). */
    public static double round(double amount) {
        return Math.round(amount * 100.0) / 100;
    }

    /** Split an amount into n equal instalments. */
    public static double instalment(double amount, int instalments) {
        return round(amount / instalments);
    }

    /** Apply a percentage fee (e.g. 2.5 for 2.5%). */
    public static double applyPercent(double amount, double percent) {
        return amount + (amount * percent / 100);
    }
}
