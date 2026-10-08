package com.example.lending.loan.export;

import org.slf4j.*;

import java.sql.*;
import java.util.*;
import java.util.Map.Entry;

public final class UniqueLabelBackfill {

    public record UniqueValueParams(String table, String idColumn, String srcColumn, String destColumn,
                                    int destColumnMaxLength) {
    }

    private static final Logger log = LoggerFactory.getLogger(UniqueLabelBackfill.class);

    private UniqueLabelBackfill() {
    }

    public static void backfill(Connection connection, UniqueValueParams params) throws SQLException {
        Map<Long, String> srcById = loadValues(connection, params, params.srcColumn());
        Map<Long, String> destById = loadValues(connection, params, params.destColumn());
        updateNonUniqueValues(connection, params, srcById, destById, new HashSet<>(destById.values()));
    }

    private static Map<Long, String> loadValues(Connection connection, UniqueValueParams params, String column)
            throws SQLException {
        Map<Long, String> values = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(String.format(
                "select %s, %s from %s where %s is not null", params.idColumn(), column, params.table(), column));
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                values.put(results.getLong(1), results.getString(2));
            }
        }
        return values;
    }

    private static void updateNonUniqueValues(
            Connection connection,
            UniqueValueParams params,
            Map<Long, String> srcById,
            Map<Long, String> destById,
            Set<String> uniqueDestValues)
            throws SQLException {
        if (srcById.isEmpty() || destById.size() == srcById.size()) {
            return;
        }
        String updateTemplate =
                String.format(
                        "update %s set %s = ? where %s = ?",
                        params.table(), params.destColumn(), params.idColumn());
        for (Entry<Long, String> idAndSrcValue : srcById.entrySet()) {
            long id = idAndSrcValue.getKey();
            String srcValue = idAndSrcValue.getValue();
            if (!destById.containsKey(id)) {
                String destValue = addValue(srcValue, params.destColumnMaxLength(), uniqueDestValues);
                try (PreparedStatement statement = connection.prepareStatement(updateTemplate)) {
                    statement.setLong(2, id);
                    statement.setString(1, destValue);

                    if (log.isInfoEnabled()) {
                        log.info(String.format("Executing %s => %s backfill update for id %d",
                                params.srcColumn(), params.destColumn(), id));
                    }
                    statement.executeUpdate();
                } catch (SQLException ex) {
                    log.error(ex.getMessage());
                    throw ex;
                }
            }
        }
    }

    private static String addValue(String srcValue, int maxLength, Set<String> uniqueValues) {
        String base = srcValue.length() > maxLength ? srcValue.substring(0, maxLength) : srcValue;
        String candidate = base;
        for (int counter = 1; uniqueValues.contains(candidate); counter++) {
            String suffix = "-" + counter;
            candidate = base.substring(0, Math.min(base.length(), maxLength - suffix.length())) + suffix;
        }
        uniqueValues.add(candidate);
        return candidate;
    }
}
