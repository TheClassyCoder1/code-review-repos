package com.acme.warehouse.service;

import com.acme.warehouse.model.NotFoundException;
import com.acme.warehouse.model.StockItem;
import com.acme.warehouse.repository.StockItemRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockAdjustmentService {

    private final StockItemRepository stockItems;
    private final JdbcTemplate jdbc;
    private final LowStockNotifier lowStockNotifier;

    public StockAdjustmentService(StockItemRepository stockItems, JdbcTemplate jdbc, LowStockNotifier lowStockNotifier) {
        this.stockItems = stockItems;
        this.jdbc = jdbc;
        this.lowStockNotifier = lowStockNotifier;
    }

    @Transactional
    public StockItem adjust(String sku, int delta, String reason) {
        StockItem item = stockItems.findBySku(sku)
                .orElseThrow(() -> new NotFoundException("stock item " + sku));
        int next = item.quantity() + delta;
        if (next < item.reserved()) {
            throw new IllegalStateException("adjustment would drop " + sku + " below reserved stock");
        }
        stockItems.setQuantity(sku, next);
        jdbc.update("INSERT INTO stock_adjustments (sku, delta, reason, created_at) VALUES (?, ?, ?, now())",
                sku, delta, reason);
        StockItem updated = item.withQuantity(next);
        if (next - item.reserved() <= item.reorderPoint()) {
            lowStockNotifier.notifyBuyer(updated);
        }
        return updated;
    }
}
