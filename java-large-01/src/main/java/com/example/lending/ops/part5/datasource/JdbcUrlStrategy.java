package com.example.lending.ops.part5.datasource;

import java.util.Map;

/** Builds a JDBC URL for a partner data source registered in the console. */
public interface JdbcUrlStrategy {

    String getJdbcUrl(String address, Map<String, String> paramsJson, String paramsStr);

    String defaultDriver();
}
