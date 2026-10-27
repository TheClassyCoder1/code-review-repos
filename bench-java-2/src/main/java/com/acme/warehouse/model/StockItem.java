package com.acme.warehouse.model;

import java.math.BigDecimal;

public record StockItem(
        long id,
        String sku,
        String name,
        int quantity,
        int reserved,
        int reorderPoint,
        BigDecimal unitCost,
        String binCode) {

    public StockItem withQuantity(int newQuantity) {
        return new StockItem(id, sku, name, newQuantity, reserved, reorderPoint, unitCost, binCode);
    }
}
