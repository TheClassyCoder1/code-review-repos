package com.example.lending.loan.part6.util;

/**
 * Masks borrower contact details before they reach a log line or an error message.
 * Only the first character of the local part and the domain survive for emails, and only
 * the last two digits survive for phone numbers.
 */
public final class PiiMasker {

    private static final String MASK = "***";

    private PiiMasker() {
    }

    /** "jane.doe@example.com" becomes "j***@example.com"; anything unparseable is fully masked. */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return MASK;
        }
        int at = email.indexOf('@');
        if (at <= 0 || at == email.length() - 1) {
            return MASK;
        }
        return email.charAt(0) + MASK + email.substring(at);
    }

    /** "+44 7700 900123" becomes "***23". */
    public static String maskPhone(String phone) {
        if (phone == null) {
            return MASK;
        }
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < 4) {
            return MASK;
        }
        return MASK + digits.substring(digits.length() - 2);
    }

    /** National identifiers are never partially revealed. */
    public static String maskNationalId(String nationalId) {
        return MASK;
    }
}
