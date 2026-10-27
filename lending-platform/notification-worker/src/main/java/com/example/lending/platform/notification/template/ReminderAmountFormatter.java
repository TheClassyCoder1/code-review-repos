package com.example.lending.platform.notification.template;

import java.text.NumberFormat;
import java.util.Locale;

/** Renders amounts for repayment-reminder and statement notifications. */
public final class ReminderAmountFormatter {

    private static final Locale UK = Locale.UK;

    private ReminderAmountFormatter() {
    }

    /** Round to cents, half-up. */
    public static double roundToCents(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    /** {@code 1234.5} becomes {@code £1,234.50}. */
    public static String format(double amount) {
        NumberFormat currency = NumberFormat.getCurrencyInstance(UK);
        return currency.format(roundToCents(amount));
    }

    public static String reminderLine(double amount, String dueDate) {
        return "Your next repayment of " + format(amount) + " is due on " + dueDate + ".";
    }
}
