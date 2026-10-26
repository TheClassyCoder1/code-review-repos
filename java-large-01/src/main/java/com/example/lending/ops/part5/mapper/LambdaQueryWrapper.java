package com.example.lending.ops.part5.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * Type-safe filter built from getter references. Mappers translate it to SQL;
 * {@link #test(Object)} evaluates it in memory for cached reference tables.
 */
public class LambdaQueryWrapper<T> {

    private final List<Predicate<T>> conditions = new ArrayList<>();

    public LambdaQueryWrapper<T> in(SFunction<T, ?> field, Collection<?> values) {
        List<?> snapshot = new ArrayList<>(values);
        conditions.add(row -> snapshot.contains(field.apply(row)));
        return this;
    }

    public boolean test(T row) {
        return conditions.stream().allMatch(c -> c.test(row));
    }
}
