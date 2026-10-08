package com.example.lending.loan.service;

import com.example.lending.loan.partner.LedgerSchemaInspector;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SchemaReportService {

    private static final String SCHEMA = "lending";

    private final DataSource dataSource;

    public SchemaReportService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Map<String, List<String>> primaryKeys(List<String> tables) throws SQLException {
        Map<String, List<String>> keys = new LinkedHashMap<>();
        try (LedgerSchemaInspector inspector = new LedgerSchemaInspector(dataSource)) {
            for (String table : tables) {
                keys.put(table, inspector.getPrimaryKeys(SCHEMA, table));
            }
        }
        return keys;
    }
}
