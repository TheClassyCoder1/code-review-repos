package com.example.lending.loan.servicing.reporting.dictionary;

import com.example.lending.loan.servicing.reporting.dictionary.ReportingColumnDialect.ColumnDoc;
import com.example.lending.loan.servicing.reporting.dictionary.ReportingColumnDialect.TablePath;

import com.example.lending.loan.servicing.common.ServicingException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/** Writes column descriptions of the reporting schema into the database catalog. */
@Service
public class DataDictionaryService {

    private static final Set<String> DOCUMENTED_SCHEMAS = Set.of("reporting", "reporting_archive");

    private final JdbcTemplate jdbcTemplate;
    private final ReportingColumnDialect dialect;

    public DataDictionaryService(JdbcTemplate jdbcTemplate, ReportingColumnDialect dialect) {
        this.jdbcTemplate = jdbcTemplate;
        this.dialect = dialect;
    }

    @Transactional
    public int applyComments(TablePath tablePath, List<ColumnDoc> columns) {
        if (!DOCUMENTED_SCHEMAS.contains(tablePath.schemaName())) {
            throw ServicingException.badRequest("Schema is not part of the data dictionary");
        }
        int applied = 0;
        for (ColumnDoc column : columns) {
            if (column.getComment() == null) {
                continue;
            }
            jdbcTemplate.execute(dialect.buildColumnCommentSQL(tablePath, column));
            applied++;
        }
        return applied;
    }

    @Transactional(readOnly = true)
    public List<String> listColumns(TablePath tablePath) {
        return jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_schema = ? AND table_name = ? "
                        + "ORDER BY ordinal_position",
                String.class, tablePath.schemaName(), tablePath.tableName());
    }
}
