package com.acme.warehouse.service;

import com.acme.warehouse.model.NotFoundException;
import com.acme.warehouse.model.StockItem;
import com.acme.warehouse.model.Transfer;
import com.acme.warehouse.repository.StockItemRepository;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

    private final JdbcTemplate jdbc;
    private final StockItemRepository stockItems;

    public TransferService(JdbcTemplate jdbc, StockItemRepository stockItems) {
        this.jdbc = jdbc;
        this.stockItems = stockItems;
    }

    @Transactional
    public long reserveStock(String sku, String fromBin, String toBin, int quantity) {
        StockItem item = stockItems.findBySku(sku)
                .orElseThrow(() -> new NotFoundException("stock item " + sku));
        int available = item.quantity() - item.reserved();
        if (available < quantity) {
            throw new IllegalStateException("insufficient stock for " + sku);
        }
        stockItems.addReserved(sku, quantity);
        return jdbc.queryForObject(
                "INSERT INTO transfers (sku, from_bin, to_bin, quantity, status) VALUES (?, ?, ?, ?, 'RESERVED') RETURNING id",
                Long.class, sku, fromBin, toBin, quantity);
    }

    public Transfer findTransfer(long transferId) {
        List<Transfer> rows = jdbc.query(
                "SELECT id, sku, from_bin, to_bin, quantity, status FROM transfers WHERE id = ?",
                (rs, n) -> new Transfer(rs.getLong("id"), rs.getString("sku"), rs.getString("from_bin"),
                        rs.getString("to_bin"), rs.getInt("quantity"), rs.getString("status")),
                transferId);
        if (rows.isEmpty()) {
            throw new NotFoundException("transfer " + transferId);
        }
        return rows.get(0);
    }

    public void completeTransfer(long transferId) {
        Transfer transfer = findTransfer(transferId);
        if (!"RESERVED".equals(transfer.status())) {
            throw new IllegalStateException("transfer " + transferId + " is " + transfer.status());
        }
        jdbc.update("UPDATE stock_items SET reserved = reserved - ?, bin_code = ? WHERE sku = ?",
                transfer.quantity(), transfer.toBin(), transfer.sku());
        jdbc.update("UPDATE bins SET used_units = used_units - ? WHERE code = ?",
                transfer.quantity(), transfer.fromBin());
        jdbc.update("UPDATE bins SET used_units = used_units + ? WHERE code = ?",
                transfer.quantity(), transfer.toBin());
        jdbc.update("UPDATE transfers SET status = 'COMPLETED' WHERE id = ?", transferId);
    }

    public List<Transfer> openTransfersForSku(String sku) {
        return jdbc.query(
                "SELECT id, sku, from_bin, to_bin, quantity, status FROM transfers WHERE sku = ? AND status = 'RESERVED'",
                (rs, n) -> new Transfer(rs.getLong("id"), rs.getString("sku"), rs.getString("from_bin"),
                        rs.getString("to_bin"), rs.getInt("quantity"), rs.getString("status")),
                sku);
    }

    @Transactional
    public void cancelTransfer(long transferId) {
        Transfer transfer = findTransfer(transferId);
        if (!"RESERVED".equals(transfer.status())) {
            throw new IllegalStateException("transfer " + transferId + " is " + transfer.status());
        }
        stockItems.releaseReserved(transfer.sku(), transfer.quantity());
        jdbc.update("UPDATE transfers SET status = 'CANCELLED' WHERE id = ?", transferId);
    }
}
