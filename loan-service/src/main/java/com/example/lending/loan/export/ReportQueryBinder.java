package com.example.lending.loan.export;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class ReportQueryBinder {

    private static final String LITERAL_QUOTE = "'";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT).withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT).withZone(ZoneOffset.UTC);

    public record Param(String className, Object value) {
        public static Param of(Object value) {
            return new Param(value == null ? null : value.getClass().getCanonicalName(), value);
        }
    }

    private ReportQueryBinder() {
    }

    public static String bind(String sqlTemplate, List<Param> params) {
        StringBuilder sql = new StringBuilder(sqlTemplate.length() + params.size() * 16);
        int next = 0;
        for (char c : sqlTemplate.toCharArray()) {
            sql.append(c == '?' ? convertToLiteralString(params.get(next++)) : String.valueOf(c));
        }
        if (next != params.size()) {
            throw new IllegalArgumentException("Parameter count does not match report query");
        }
        return sql.toString();
    }

    private static String convertToLiteralString(Param param) {
        Object value = param.value();
        if (value == null) {
            return "NULL";
        }

        if (value instanceof String) {
            if (param.className().equals(BigDecimal.class.getCanonicalName())) {
                return (String) value;
            }
            return LITERAL_QUOTE + (String) value + LITERAL_QUOTE;
        } else if (value instanceof java.sql.Date) {
            return String.format(Locale.ROOT, "date'%s'",
                    DATE_FORMAT.format(Instant.ofEpochMilli(((Date) value).getTime())));
        } else if (value instanceof Timestamp) {
            return String.format(Locale.ROOT, "timestamp'%s'",
                    TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(((Timestamp) value).getTime())));
        } else {
            return String.valueOf(value);
        }
    }
}
