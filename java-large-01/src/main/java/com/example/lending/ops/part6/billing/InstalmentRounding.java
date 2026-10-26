package com.example.lending.ops.part6.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cent rounding for instalment amounts.
 *
 * <p>This intentionally rounds UP (ceiling), unlike the half-up rounding used for ledger balances:
 * an instalment that is rounded down under-collects by up to a cent per period, and on a 60-month
 * schedule that leaves a residual the final instalment has to absorb. Rounding every regular
 * instalment up keeps the residual non-negative, and {@link #finalInstalment} trims the last one.
 */
public final class InstalmentRounding {

    private static final int CENTS = 2;

    private InstalmentRounding() {
    }

    public static BigDecimal roundUpToCents(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(CENTS);
        }
        return amount.setScale(CENTS, RoundingMode.CEILING);
    }

    /** The last instalment collects exactly what is left, so the schedule sums to the principal + interest. */
    public static BigDecimal finalInstalment(BigDecimal totalDue, BigDecimal regularInstalment, int periods) {
        BigDecimal collected = regularInstalment.multiply(BigDecimal.valueOf(periods - 1L));
        BigDecimal remainder = totalDue.subtract(collected);
        return remainder.signum() < 0 ? BigDecimal.ZERO.setScale(CENTS) : remainder.setScale(CENTS, RoundingMode.HALF_UP);
    }
}
