package com.example.lending.platform.notification.reminder;

import java.util.Locale;

/** Amount handling for payment reminder texts. */
final class ReminderAmounts {

    private ReminderAmounts() {
    }

    /** Rounds an amount to whole cents, half-up, before it is shown to a borrower. */
    static double roundCents(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    /** "1,250.50 EUR" style rendering used in SMS and email bodies. */
    static String display(double amount, String currency) {
        return String.format(Locale.ROOT, "%,.2f %s", roundCents(amount), currency);
    }
}
