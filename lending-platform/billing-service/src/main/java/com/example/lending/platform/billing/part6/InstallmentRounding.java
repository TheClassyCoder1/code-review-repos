package com.example.lending.platform.billing.part6;

/**
 * Cent rounding for repayment installments.
 *
 * This deliberately differs from both MoneyUtil variants (shared half-up and
 * billing floor): installments are rounded UP to the next cent. If they were
 * rounded half-up or down, n equal installments could sum to less than the
 * principal and leave a residual balance of a few cents after the final
 * payment, which then shows as an arrears item. Rounding up guarantees the
 * schedule covers the principal; the final installment absorbs the surplus
 * (see {@link InstallmentScheduleCalculator}).
 */
public final class InstallmentRounding {

    private InstallmentRounding() {
    }

    /** Round up to the next whole cent. */
    public static double roundUp(double amount) {
        return Math.ceil(amount * 100.0 - 1e-9) / 100.0;
    }
}
