package com.example.lending.loan.ops.reports;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportCatalogService {

    private final JdbcTemplate jdbcTemplate;

    public ReportCatalogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ReportTableInfo> listTables(String database) {
        return jdbcTemplate.queryForList(
                        "SELECT table_name AS \"NAME\", owner AS \"OWNER\", "
                                + "COALESCE(create_time, 0) AS \"CREATE_TIME\", "
                                + "COALESCE(last_access_time, create_time, 0) AS \"LAST_ACCESS_TIME\" "
                                + "FROM lending.report_tables WHERE database_name = ? ORDER BY table_name",
                        database)
                .stream()
                .map(row -> ReportTableConversions.mapToReportTableInfo(row, database))
                .toList();
    }
}
