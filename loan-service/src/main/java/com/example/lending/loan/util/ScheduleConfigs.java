package com.example.lending.loan.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ScheduleConfigs {

    private static final Pattern TERM = Pattern.compile("\\d+x\\d+");
    private static final Pattern CRON = Pattern.compile("cron=([^;]+)");

    public record Term(int installments, int intervalDays) {
    }

    private ScheduleConfigs() {
    }

    public static String cronOf(String config) {
        if (config == null) {
            return null;
        }
        Matcher matcher = CRON.matcher(config);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    public static Term termOf(String config, Term def) {
        return config == null ? def : getTerm(config, def);
    }

    public static Term getTerm(String txt, Term def) {
        Matcher matcher = TERM.matcher(txt);

        if (matcher.find()) {
            String foundTerm = txt.substring(matcher.start(), matcher.end());
            String[] parts = foundTerm.split("x");
            return new Term(toInt(parts[0]), toInt(parts[1]));
        }

        return def;
    }

    private static int toInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
