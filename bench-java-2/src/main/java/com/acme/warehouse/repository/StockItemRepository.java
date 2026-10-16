package com.acme.warehouse.repository;

import com.acme.warehouse.model.StockItem;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class StockItemRepository {

    private static final RowMapper<StockItem> MAPPER = (rs, rowNum) -> new StockItem(
            rs.getLong("id"),
            rs.getString("sku"),
            rs.getString("name"),
            rs.getInt("quantity"),
            rs.getInt("reserved"),
            rs.getInt("reorder_point"),
            rs.getBigDecimal("unit_cost"),
            rs.getString("bin_code"));

    private final JdbcTemplate jdbc;

    public StockItemRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<StockItem> findBySku(String sku) {
        List<StockItem> rows = jdbc.query("SELECT * FROM stock_items WHERE sku = ?", MAPPER, sku);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    public List<StockItem> findAll() {
        return jdbc.query("SELECT * FROM stock_items ORDER BY sku", MAPPER);
    }

    public void lockBySku(String sku) {
        jdbc.queryForObject("SELECT id FROM stock_items WHERE sku = ? FOR UPDATE", Long.class, sku);
    }

    public List<StockItem> fetchPage(int offset, int limit) {
        return jdbc.query(
                "SELECT * FROM stock_items ORDER BY sku LIMIT ? OFFSET ?",
                MAPPER,
                limit + 1,
                offset);
    }

    public void setQuantity(String sku, int quantity) {
        jdbc.update("UPDATE stock_items SET quantity = ? WHERE sku = ?", quantity, sku);
    }

    public int addReserved(String sku, int quantity) {
        return jdbc.update("UPDATE stock_items SET reserved = reserved + ? WHERE sku = ?", quantity, sku);
    }

    public int applyDelta(String sku, int delta) {
        Integer current = jdbc.queryForObject(
                "SELECT quantity FROM stock_items WHERE sku = ?", Integer.class, sku);
        int next = current + delta;
        jdbc.update("UPDATE stock_items SET quantity = ? WHERE sku = ?", next, sku);
        return next;
    }

    public void releaseReserved(String sku, int quantity) {
        jdbc.update("UPDATE stock_items SET reserved = GREATEST(reserved - ?, 0) WHERE sku = ?", quantity, sku);
    }

    public void upsert(String sku, String name, int quantity, java.math.BigDecimal unitCost) {
        jdbc.update("""
                INSERT INTO stock_items (sku, name, quantity, unit_cost) VALUES (?, ?, ?, ?)
                ON CONFLICT (sku) DO UPDATE SET name = EXCLUDED.name, quantity = EXCLUDED.quantity,
                    unit_cost = EXCLUDED.unit_cost
                """, sku, name, quantity, unitCost);
    }

    public List<StockItem> findBelowReorderPoint(int limit) {
        return jdbc.query(
                "SELECT * FROM stock_items WHERE quantity - reserved <= reorder_point ORDER BY sku LIMIT ?",
                MAPPER,
                limit);
    }
}
