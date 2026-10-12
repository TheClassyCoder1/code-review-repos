package com.example.lending.loan.servicing.reporting.dictionary;

import com.example.lending.loan.servicing.reporting.dictionary.ReportingColumnDialect.ColumnDoc;
import com.example.lending.loan.servicing.reporting.dictionary.ReportingColumnDialect.TablePath;

import com.example.lending.loan.servicing.common.ServicingResult;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Data dictionary maintenance for the reporting warehouse. */
@RestController
@RequestMapping("/servicing/reporting/dictionary/{schema}/{table}")
public class DataDictionaryController {

    private final DataDictionaryService dictionaryService;

    public DataDictionaryController(DataDictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('dictionary:read')")
    public ServicingResult<List<String>> columns(@PathVariable String schema, @PathVariable String table) {
        return ServicingResult.ok(dictionaryService.listColumns(new TablePath(schema, table)));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('dictionary:edit')")
    public ServicingResult<Integer> comment(@PathVariable String schema, @PathVariable String table,
                                            @RequestBody List<@Valid ColumnDoc> columns) {
        return ServicingResult.ok(dictionaryService.applyComments(new TablePath(schema, table), columns));
    }
}
