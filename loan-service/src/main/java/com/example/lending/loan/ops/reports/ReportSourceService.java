package com.example.lending.loan.ops.reports;

import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ReportSourceService {

    private final DataSource dataSource;

    public ReportSourceService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<ReportSourceInspector.ColumnInfo> describe(String schema, String table) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            if (!existingTables(conn, schema).contains(table)) {
                throw new IllegalArgumentException("Unknown report source");
            }
            return new ReportSourceInspector(conn).getColumns(schema, table);
        }
    }

    private static Set<String> existingTables(Connection conn, String schema) throws SQLException {
        Set<String> tables = new HashSet<>();
        try (ResultSet schemas = conn.getMetaData().getSchemas()) {
            boolean known = false;
            while (schemas.next()) {
                known |= schemas.getString("TABLE_SCHEM").equals(schema);
            }
            if (!known) {
                return tables;
            }
        }
        try (ResultSet rs = conn.getMetaData().getTables(null, schema, "%", new String[] {"TABLE", "VIEW"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        return tables;
    }
}
