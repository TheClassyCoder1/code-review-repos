package com.example.lending.ops.part6.common;

import com.example.lending.ops.common.StringUtils;

/**
 * Text checks for values coming out of partner feeds and legacy form posts, where a missing value
 * often arrives as the literal string "null". Delegates to the console's shared {@link StringUtils}
 * so the definition of "empty" stays in one place.
 */
public final class OpsText {

    private OpsText() {
    }

    public static boolean isMissing(Object value) {
        return StringUtils.oConvertUtils.isEmpty(value);
    }

    public static boolean isPresent(Object value) {
        return !isMissing(value);
    }

    public static String orDefault(Object value, String fallback) {
        return isMissing(value) ? fallback : value.toString().trim();
    }
}
