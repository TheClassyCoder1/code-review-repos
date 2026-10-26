package com.example.lending.ops.part4.engine;

import java.util.HashMap;
import java.util.Map;

public final class UtilMisc {

    private UtilMisc() { }

    /** Mutable map from alternating key/value arguments. */
    public static Map<String, Object> toMap(Object... keysAndValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i + 1 < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }
}
