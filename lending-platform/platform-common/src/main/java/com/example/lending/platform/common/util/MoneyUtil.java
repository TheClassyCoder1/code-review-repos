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

    public static final int CENT_SCALE = 2;

    private MoneyUtil() {
    }

    /** Round to cents, half-up, via BigDecimal so 1.005 rounds to 1.01 rather than 1.00. */
    public static double roundHalfUp(double amount) {
        return roundHalfUp(BigDecimal.valueOf(amount)).doubleValue();
    }

    /** Round to cents, half-up. */
    public static BigDecimal roundHalfUp(BigDecimal amount) {
        return amount.setScale(CENT_SCALE, RoundingMode.HALF_UP);
    }
}
