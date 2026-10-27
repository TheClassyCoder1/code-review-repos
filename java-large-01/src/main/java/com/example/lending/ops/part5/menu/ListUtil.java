package com.example.lending.ops.part5.menu;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/** List transformation helpers. */
public final class ListUtil {

    private ListUtil() {
    }

    public static <T, R> List<R> map(Collection<T> source, Function<? super T, ? extends R> mapper) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream().map(mapper).collect(Collectors.toList());
    }
}
