package com.example.lending.ops.part6.common;

/**
 * Masking helpers for anything that may end up in a log line, an audit row or an alert.
 * Every helper is null-safe and never returns more than a few characters of the original value.
 */
public final class PiiMask {

    private static final String REDACTED = "***";

    private PiiMask() {
    }

    /** {@code jane.doe@example.com} becomes {@code j***@e***.com}. */
    public static String email(String email) {
        if (email == null || email.isBlank()) {
            return REDACTED;
        }
        int at = email.indexOf('@');
        if (at <= 0 || at == email.length() - 1) {
            return REDACTED;
        }
        String domain = email.substring(at + 1);
        int dot = domain.lastIndexOf('.');
        String tld = dot >= 0 ? domain.substring(dot) : "";
        return email.charAt(0) + REDACTED + "@" + domain.charAt(0) + REDACTED + tld;
    }

    /** Keeps the last two digits only: {@code +44 7700 900123} becomes {@code ***23}. */
    public static String phone(String phone) {
        if (phone == null) {
            return REDACTED;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return REDACTED;
        }
        return REDACTED + digits.substring(digits.length() - 2);
    }

    /** National IDs are never partially shown; only their presence is. */
    public static String nationalId(String nationalId) {
        return nationalId == null || nationalId.isBlank() ? "<none>" : REDACTED;
    }

    /** First letter of each name part: {@code Jane Doe} becomes {@code J. D.}. */
    public static String name(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return REDACTED;
        }
        StringBuilder sb = new StringBuilder();
        for (String part : fullName.trim().split("\\s+")) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(part.charAt(0)).append('.');
        }
        return sb.toString();
    }
}
