package com.example.lending.ops.part5.mapper;

import java.io.Serializable;
import java.util.function.Function;

/** Serializable getter reference, e.g. {@code SysDept::getDeptId}, resolved to a column by the wrapper. */
@FunctionalInterface
public interface SFunction<T, R> extends Function<T, R>, Serializable {
}
