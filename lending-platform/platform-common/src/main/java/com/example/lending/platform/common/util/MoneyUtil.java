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

    /** Round half-up to {@code scale} decimal places (2 = cents, 4 = rate precision). */
    public static double round(double amount, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("scale must not be negative");
        }
        return BigDecimal.valueOf(amount).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
