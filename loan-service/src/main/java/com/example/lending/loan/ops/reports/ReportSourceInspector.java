package com.example.lending.loan.ops.reports;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReportSourceInspector {

    public record ColumnInfo(int index, String name, String type, boolean primaryKey) {
    }

    private final Connection conn;

    ReportSourceInspector(Connection conn) {
        this.conn = conn;
    }

    public List<ColumnInfo> getColumns(String schemaname, String table)
            throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();
        String columnSql = "SELECT * FROM " + schemaname + "." + table + " WHERE 1 = 2";
        PreparedStatement ps = null;
        ResultSet rs = null;
        ResultSetMetaData meta;
        try {
            List<String> primaryKeys = getPrimaryKeys(schemaname, table);
            ps = conn.prepareStatement(columnSql);
            rs = ps.executeQuery();
            meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            for (int i = 1; i < columnCount + 1; i++) {
                columns.add(new ColumnInfo(i, meta.getColumnName(i), meta.getColumnTypeName(i),
                        primaryKeys.contains(meta.getColumnName(i))));
            }
        } finally {
            closeResource(ps, rs);
        }
        return columns;
    }

    private List<String> getPrimaryKeys(String schemaname, String table) throws SQLException {
        List<String> keys = new ArrayList<>();
        try (ResultSet rs = conn.getMetaData().getPrimaryKeys(null, schemaname, table)) {
            while (rs.next()) {
                keys.add(rs.getString("COLUMN_NAME"));
            }
        }
        return keys;
    }

    private static void closeResource(PreparedStatement ps, ResultSet rs) throws SQLException {
        try {
            if (rs != null) {
                rs.close();
            }
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }
}
