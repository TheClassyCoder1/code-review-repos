package com.example.lending.ops.common;

import java.util.HashMap;

public final class StringUtils {

    public static boolean isEmpty(CharSequence cs) { return cs == null || cs.length() == 0; }

    public static boolean isBlank(CharSequence cs) { return cs == null || cs.toString().isBlank(); }

    public static boolean isNotBlank(CharSequence cs) { return !isBlank(cs); }

    public static final class oConvertUtils {
        public static boolean isEmpty(Object v) { return v == null || v.toString().isBlank() || "null".equals(v.toString()); }
    }

    public static final class Maps {
        public static <K, V> HashMap<K, V> newHashMap() { return new HashMap<>(); }
    }
}
