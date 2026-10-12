package com.example.lending.loan.servicing.recon.amounts;

/** Helpers for amounts stored as 16-digit decimal words (bank file amount fields). */
public final class DecimalWords {

    public static final int LONGWORD_DECIMAL_DIGITS = 16;

    private DecimalWords() {
    }

    public static int fastLongWordTrailingZeroCount(long longWord) {

        if (longWord == 0) {
            return LONGWORD_DECIMAL_DIGITS;
        }

        long factor = 10;
        for (int i = 0; i < LONGWORD_DECIMAL_DIGITS; i++) {
            if (longWord % factor != 0) {
                return i;
            }
            factor *= 10;
        }
        return 0;
    }

    /** Number of decimal places that can be dropped from an amount in minor units without losing value. */
    public static int removableScale(long amountMinor, int scale) {
        return Math.min(scale, fastLongWordTrailingZeroCount(Math.abs(amountMinor)));
    }
}
