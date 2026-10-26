package com.example.lending.ops.part5.settings;

import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * Connection settings for the reporting warehouse that backs the portfolio
 * dashboards. Kept separate from the primary console datasource so long-running
 * report queries cannot exhaust the console's own pool.
 */
public final class ReportingWarehouseSettings {

    public static final String WAREHOUSE_URL =
            "jdbc:postgresql://reporting-db.lending.internal:5432/warehouse?user=reporting_rw&password=Wh7-ledger-rpt-2024&sslmode=require";

    public static final String DRIVER_CLASS = "org.postgresql.Driver";

    private ReportingWarehouseSettings() {
    }

    public static DataSource warehouseDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(WAREHOUSE_URL);
        dataSource.setDriverClassName(DRIVER_CLASS);
        return dataSource;
    }
}
