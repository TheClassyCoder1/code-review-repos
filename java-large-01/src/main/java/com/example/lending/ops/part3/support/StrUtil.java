package com.example.lending.ops.part3.support;

import java.util.*;

/** String and collection helpers with the semantics the ported admin code expects. */
public final class StrUtil {
    private StrUtil() { }
    public static boolean isNotBlank(CharSequence v) { return v != null && !v.toString().isBlank(); }
    public static boolean equalsAnyIgnoreCase(CharSequence v, CharSequence... candidates) {
        return v != null && Arrays.stream(candidates).anyMatch(c -> c != null && c.toString().equalsIgnoreCase(v.toString()));
    }
    public static final class CollUtil { public static boolean isEmpty(Collection<?> c) { return c == null || c.isEmpty(); } }
}
