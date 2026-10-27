package com.example.lending.loan.part6;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Limits applied to borrower loan exports. Exposed as a named bean so the
 * limits can be referenced from configuration expressions.
 */
@Component("loanExportPolicy")
public class LoanExportPolicy {

    static final int ABSOLUTE_MAX_ROWS = 20_000;

    private final int configuredMaxRows;

    public LoanExportPolicy(@Value("${lending.export.max-rows:5000}") int configuredMaxRows) {
        this.configuredMaxRows = configuredMaxRows;
    }

    /** Maximum number of rows allowed in one export file. */
    public int maxRowsPerExport() {
        return Math.min(Math.max(configuredMaxRows, 1), ABSOLUTE_MAX_ROWS);
    }

    /** Whether an export of the given size has to be split across files. */
    public boolean requiresSplit(int rows) {
        return rows > maxRowsPerExport();
    }
}
