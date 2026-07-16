package com.example.lending.platform.billing.util;

/**
 * TRAP (copy-shadow): SAME simple name "MoneyUtil" as the shared
 * com.example.lending.platform.common.util.MoneyUtil. This billing copy rounds DOWN (floor),
 * not half-up — subtly different behavior. Not the shared class.
 */
public final class MoneyUtil {

    private MoneyUtil() {
    }

    public static double round(double amount) {
        return Math.floor(amount * 100.0) / 100.0;
    }
}
