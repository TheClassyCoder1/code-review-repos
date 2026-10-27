package com.example.lending.risk.part6.report;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Date rendering shared by the exposure reports. All report dates are UTC calendar days. */
public final class ReportDates {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter REPORT_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private ReportDates() {
    }

    /** 2026-10-26T23:59:00Z becomes "2026-10-26". */
    public static String formatDay(Instant instant) {
        if (instant == null) {
            return "";
        }
        return DAY.format(instant);
    }

    /** Date column used in the CSV export, e.g. "2026-10-26". */
    public static String toReportDate(Instant instant) {
        if (instant == null) {
            return "";
        }
        return REPORT_DATE.format(instant);
    }
}
