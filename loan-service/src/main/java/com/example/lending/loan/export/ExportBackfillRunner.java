package com.example.lending.loan.export;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;

@Component
@ConditionalOnProperty(name = "exports.backfill.enabled", havingValue = "true")
public class ExportBackfillRunner implements ApplicationRunner {

    private static final UniqueLabelBackfill.UniqueValueParams REPORT_EXPORT_LABELS =
            new UniqueLabelBackfill.UniqueValueParams("lending.report_exports", "id", "report_type", "label", 120);

    private final DataSource dataSource;

    public ExportBackfillRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            UniqueLabelBackfill.backfill(connection, REPORT_EXPORT_LABELS);
            connection.commit();
        }
    }
}
