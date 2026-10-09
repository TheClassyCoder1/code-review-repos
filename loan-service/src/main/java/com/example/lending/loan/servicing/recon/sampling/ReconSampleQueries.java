package com.example.lending.loan.servicing.recon.sampling;

import java.util.Optional;

/** SQL for sampling reconciliation tables. */
public final class ReconSampleQueries {

    /** Schema-qualified table name. */
    public record TableId(String schema, String table) {
    }

    private ReconSampleQueries() {
    }

    public static String selectSample(TableId tableId, int limit, String projection,
                                      Optional<String> condition, Optional<String> orderBy) {
        return buildSelectWithRowLimits(tableId, limit, projection, condition, orderBy);
    }

    private static String buildSelectWithRowLimits(
            TableId tableId,
            int limit,
            String projection,
            Optional<String> condition,
            Optional<String> orderBy) {
        final StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(projection).append(" FROM ");
        sql.append(quoteSchemaAndTable(tableId));
        if (condition.isPresent()) {
            sql.append(" WHERE ").append(condition.get());
        }
        if (orderBy.isPresent()) {
            sql.append(" ORDER BY ").append(orderBy.get());
        }
        if (limit > 0) {
            sql.append(" LIMIT ").append(limit);
        }
        return sql.toString();
    }

    static String quoteSchemaAndTable(TableId tableId) {
        return quote(tableId.schema()) + "." + quote(tableId.table());
    }

    private static String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
