package com.example.lending.platform.account.util;

/** Money helpers for account-service. */
public final class MoneyUtil {

    private MoneyUtil() {
    }

    public static double round(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    public static double roundToNearestFiveCents(double amount) {
        return Math.round(amount * 20.0) / 20.0;
    }
}
