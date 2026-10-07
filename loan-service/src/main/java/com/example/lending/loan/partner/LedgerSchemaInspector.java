package com.example.lending.loan.partner;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LedgerSchemaInspector implements AutoCloseable {

    private final Connection conn;

    public LedgerSchemaInspector(DataSource dataSource) throws SQLException {
        this.conn = dataSource.getConnection();
    }

    public List<String> getPrimaryKeys(String schema, String table) throws SQLException {
        ResultSet rs = null;
        List<String> primaryKeys = new ArrayList<>();
        DatabaseMetaData dbMeta = conn.getMetaData();
        rs = dbMeta.getPrimaryKeys(null, schema, table);
        while (rs.next()) {
            primaryKeys.add(rs.getString("COLUMN_NAME"));
        }
        return primaryKeys;
    }

    @Override
    public void close() throws SQLException {
        conn.close();
    }
}
