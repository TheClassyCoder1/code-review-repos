package com.example.lending.ops.part5.common;

import java.util.ArrayList;
import java.util.Collection;

/** Small collection helpers shared by the reporting and admin modules. */
public final class CollUtil {

    private CollUtil() {
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static <T> ArrayList<T> newArrayList() {
        return new ArrayList<>();
    }
}
