package com.example.lending.ops.part3.sortgroup;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.*;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/** Opens streaming cursors on the transaction's connection; only inside a transaction, and closed when it completes. */
@Component
@RequiredArgsConstructor
public class TransactionBoundCursors {
    private final DataSource dataSource;
    public <T> Cursor<T> open(String sql, RowMapper<T> rowMapper) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) throw new IllegalStateException("cursors need a transaction");
        PreparedStatement statement = jdbc(() -> DataSourceUtils.getConnection(dataSource).prepareStatement(sql));
        ResultSet rs = jdbc(() -> { statement.setFetchSize(500); return statement.executeQuery(); });
        Cursor<T> cursor = new Cursor<>() {
            public Iterator<T> iterator() {
                return new Iterator<>() {
                    Boolean ahead;
                    int row;
                    public boolean hasNext() { return ahead != null ? ahead : (ahead = jdbc(rs::next)); }
                    public T next() {
                        if (!hasNext()) throw new NoSuchElementException();
                        ahead = null;
                        return jdbc(() -> rowMapper.mapRow(rs, row++));
                    }
                };
            }
            public void close() { jdbc(() -> { statement.close(); return null; }); }
        };
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            public void afterCompletion(int status) { cursor.close(); }
        });
        return cursor;
    }
    private interface Op<R> { R get() throws SQLException; }
    private static <R> R jdbc(Op<R> op) {
        try { return op.get(); } catch (SQLException e) { throw new IllegalStateException("cursor query failed", e); }
    }
}
