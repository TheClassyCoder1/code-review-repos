package com.example.lending.ops.part4.engine;

import java.util.Locale;
import java.util.Map;

/** Label lookup; this console ships no bundles yet, so the key itself is the message. */
public final class UtilProperties {

    private UtilProperties() { }

    public static String getMessage(String resource, String key, Locale locale) {
        return key;
    }

    public static String getMessage(String resource, String key, Map<String, ? extends Object> args, Locale locale) {
        return args.isEmpty() ? key : key + " " + args;
    }
}
