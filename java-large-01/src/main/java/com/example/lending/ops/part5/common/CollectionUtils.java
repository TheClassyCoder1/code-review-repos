package com.example.lending.ops.part5.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Null-tolerant collection helpers. */
public final class CollectionUtils {

    private CollectionUtils() {
    }

    @SafeVarargs
    public static <E> List<E> asList(E... elements) {
        if (elements == null || elements.length == 0) {
            return Collections.emptyList();
        }

        // Integer overflow does not occur when a large array is passed in because the list array already exists
        return Arrays.asList(elements);
    }
}
