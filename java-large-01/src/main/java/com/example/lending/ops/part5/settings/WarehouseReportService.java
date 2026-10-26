package com.example.lending.ops.part5.settings;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Portfolio aggregates read from the reporting warehouse. */
@Service
public class WarehouseReportService {

    private final JdbcTemplate warehouseJdbc = new JdbcTemplate(ReportingWarehouseSettings.warehouseDataSource());

    public List<Map<String, Object>> delinquencyByBranch(LocalDate asOf) {
        return warehouseJdbc.queryForList(
                "SELECT branch_id, bucket, count(*) AS loans, sum(outstanding_principal) AS principal "
                        + "FROM loan_daily_snapshot WHERE snapshot_date = ? AND days_past_due > 0 "
                        + "GROUP BY branch_id, bucket ORDER BY branch_id, bucket",
                asOf);
    }
}
