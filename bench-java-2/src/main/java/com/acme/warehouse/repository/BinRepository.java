package com.acme.warehouse.repository;

import com.acme.warehouse.model.Bin;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class BinRepository {

    private static final RowMapper<Bin> MAPPER = (rs, rowNum) -> new Bin(
            rs.getLong("id"),
            rs.getString("code"),
            rs.getString("zone"),
            rs.getInt("capacity"),
            rs.getInt("used_units"),
            rs.getInt("pick_sequence"));

    private final JdbcTemplate jdbc;

    public BinRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Bin> findByCode(String code) {
        List<Bin> rows = jdbc.query("SELECT * FROM bins WHERE code = ?", MAPPER, code);
        if (rows.size() > 1) {
            throw new IllegalStateException("duplicate bins for code " + code);
        }
        return rows.stream().findFirst();
    }

    public List<Bin> findAll() {
        return jdbc.query("SELECT * FROM bins ORDER BY pick_sequence, code", MAPPER);
    }

    public List<Bin> findByZone(String zone) {
        return jdbc.query("SELECT * FROM bins WHERE zone = ? ORDER BY pick_sequence", MAPPER, zone);
    }

    public boolean createIfAbsent(String code, String zone, int capacity) {
        Integer existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM bins WHERE code = ?", Integer.class, code);
        if (existing != null && existing > 0) {
            return false;
        }
        jdbc.update("INSERT INTO bins (code, zone, capacity) VALUES (?, ?, ?)", code, zone, capacity);
        return true;
    }

    public int countItemsInBin(String code) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM stock_items WHERE bin_code = ?", Integer.class, code);
        return count == null ? 0 : count;
    }

    public int addUsedUnits(String code, int units) {
        Integer used = jdbc.queryForObject(
                "SELECT used_units FROM bins WHERE code = ?", Integer.class, code);
        int next = used + units;
        jdbc.update("UPDATE bins SET used_units = ? WHERE code = ?", next, code);
        return next;
    }

    public void setPickSequence(String code, int sequence) {
        int updated = jdbc.update("UPDATE bins SET pick_sequence = ? WHERE code = ?", sequence, code);
        if (updated == 0) {
            throw new IllegalArgumentException("unknown bin " + code);
        }
    }

    public void moveContents(String fromCode, String toCode) {
        jdbc.update("UPDATE stock_items SET bin_code = ? WHERE bin_code = ?", toCode, fromCode);
        jdbc.update("""
                UPDATE bins SET used_units = used_units + (SELECT used_units FROM bins WHERE code = ?)
                WHERE code = ?
                """, fromCode, toCode);
        jdbc.update("UPDATE bins SET used_units = 0 WHERE code = ?", fromCode);
    }
}
