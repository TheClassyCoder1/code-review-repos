package com.example.lending.loan.servicing.reporting.scripts;

import java.util.Locale;
import java.util.regex.Pattern;

/** Statement kinds accepted in saved report scripts. */
public enum ReportSqlCommand {

    SELECT("(SELECT|WITH)\\b.*", true),
    EXPLAIN("EXPLAIN\\b.*", true),
    SET("SET\\s+(statement_timeout|search_path)\\b.*", true),
    OTHER(".*", false);

    private final Pattern pattern;
    private final boolean allowed;

    ReportSqlCommand(String regex, boolean allowed) {
        this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        this.allowed = allowed;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public static ReportSqlCommand of(String statement) {
        String normalized = statement.strip().toUpperCase(Locale.ROOT);
        for (ReportSqlCommand command : values()) {
            if (command.pattern.matcher(normalized).matches()) {
                return command;
            }
        }
        return OTHER;
    }
}
