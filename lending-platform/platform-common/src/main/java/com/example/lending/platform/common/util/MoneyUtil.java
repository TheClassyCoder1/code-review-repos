package com.example.lending.platform.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
    public static double roundToCents(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    /** Round to cents (half-up) without going through binary floating point. */
    public static BigDecimal roundToCents(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    /** Whole minor units (cents) for an amount already rounded to cents. */
    public static long toMinorUnits(double amount) {
        return Math.round(roundToCents(amount) * 100.0);
    }
}
