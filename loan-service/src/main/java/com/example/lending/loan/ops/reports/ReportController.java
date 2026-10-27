package com.example.lending.loan.ops.reports;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/ops/reports")
public class ReportController {

    private final ReportCatalogService catalogService;
    private final ReportSourceService sourceService;

    public ReportController(ReportCatalogService catalogService, ReportSourceService sourceService) {
        this.catalogService = catalogService;
        this.sourceService = sourceService;
    }

    @GetMapping("/tables")
    public List<ReportTableInfo> tables(@RequestParam String database) {
        return catalogService.listTables(database);
    }

    @GetMapping("/columns")
    public List<ReportSourceInspector.ColumnInfo> columns(@RequestParam String schema, @RequestParam String table)
            throws SQLException {
        return sourceService.describe(schema, table);
    }
}
