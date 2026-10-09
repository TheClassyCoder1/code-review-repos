package com.example.lending.loan.servicing.reporting.dictionary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Component;

/** PostgreSQL statements for the reporting data dictionary. */
@Component
public class ReportingColumnDialect {

    /** Schema-qualified table of the reporting warehouse. */
    public record TablePath(String schemaName, String tableName) {
    }

    /** Description of a reporting column maintained by data stewards. */
    public static class ColumnDoc {

        @NotBlank
        @Size(max = 63)
        private String name;

        @Size(max = 1000)
        private String comment;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }

    public String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    public String tableIdentifier(TablePath tablePath) {
        return quoteIdentifier(tablePath.schemaName()) + "." + quoteIdentifier(tablePath.tableName());
    }

    public String buildColumnCommentSQL(TablePath tablePath, ColumnDoc column) {
        return String.format(
                "COMMENT ON COLUMN %s.%s IS '%s'",
                tableIdentifier(tablePath), quoteIdentifier(column.getName()), column.getComment());
    }
}
