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
        return Math.round(amount * 100.0) / 100.0;
    }

    /**
     * Round to the nearest 5 cents — cash handling in jurisdictions that have retired the 1c coin.
     * Callers that settle in cash must round the FINAL payable only, never intermediate ledger
     * amounts, or the rounding compounds.
     */
    public static double roundToNearestFiveCents(double amount) {
        return Math.round(amount * 20.0) / 20.0;
    }
}
