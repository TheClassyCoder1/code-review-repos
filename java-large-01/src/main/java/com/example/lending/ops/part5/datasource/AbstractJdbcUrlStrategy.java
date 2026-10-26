package com.example.lending.ops.part5.datasource;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Shared helpers for vendor-specific JDBC URL strategies. */
public abstract class AbstractJdbcUrlStrategy implements JdbcUrlStrategy {

    /** Normalises a user-supplied "k=v&k2=v2" or "k=v;k2=v2" string into ";k=v;k2=v2". */
    protected String getConnectParams(String paramsStr) {
        return Arrays.stream(paramsStr.split("[&;]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> ";" + s)
                .collect(Collectors.joining());
    }
}
