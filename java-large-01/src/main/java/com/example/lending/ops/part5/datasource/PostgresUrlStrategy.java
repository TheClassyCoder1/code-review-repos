package com.example.lending.ops.part5.datasource;

import java.util.Map;

/** PostgreSQL URLs for partner warehouses. Credentials are supplied separately, never in the URL. */
public class PostgresUrlStrategy extends AbstractJdbcUrlStrategy {

    @Override
    public String getJdbcUrl(String address, Map<String, String> paramsJson, String paramsStr) {
        return "jdbc:postgresql://" + address + "/" + paramsJson.getOrDefault("databaseName", "");
    }

    @Override
    public String defaultDriver() {
        return "org.postgresql.Driver";
    }
}
