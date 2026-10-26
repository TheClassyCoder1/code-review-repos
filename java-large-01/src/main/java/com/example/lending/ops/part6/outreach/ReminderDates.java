package com.example.lending.ops.part6.outreach;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Formats due dates for payment reminder copy. */
final class ReminderDates {

    private static final ZoneId ZONE = ZoneId.of("Europe/London");
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private ReminderDates() {
    }

    static String toDisplay(Instant instant) {
        if (instant == null) {
            return "";
        }
        return FORMAT.format(instant.atZone(ZONE));
    }
}
