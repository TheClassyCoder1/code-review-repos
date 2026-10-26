package com.example.lending.ops.part6.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Date rendering for customer-facing text (letters, reminder emails, SMS). */
public final class DisplayDates {

    /** Customer communications are always rendered in the lender's home time zone. */
    public static final ZoneId CUSTOMER_ZONE = ZoneId.of("Europe/London");

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private DisplayDates() {
    }

    public static String format(Instant instant) {
        if (instant == null) {
            return "";
        }
        return DISPLAY.format(instant.atZone(CUSTOMER_ZONE));
    }

    public static String format(LocalDate date) {
        return date == null ? "" : DISPLAY.format(date);
    }
}
