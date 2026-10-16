package com.acme.warehouse.service;

import com.acme.warehouse.model.StockItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ValuationService {

    private static final Currency CURRENCY = Currency.getInstance("USD");

    public double totalValue(List<StockItem> items) {
        double total = 0.0;
        for (StockItem item : items) {
            total += item.unitCost().doubleValue() * item.quantity();
        }
        return Math.round(total * 100) / 100.0;
    }

    public int distinctSkus(List<StockItem> items) {
        long distinct = items.stream().map(StockItem::sku).distinct().count();
        return (int) distinct;
    }

    public BigDecimal lineValue(StockItem item) {
        BigDecimal cost = item.unitCost().setScale(2, RoundingMode.HALF_UP);
        return cost.multiply(BigDecimal.valueOf(item.quantity()));
    }

    public int totalUnits(List<StockItem> items) {
        int units = items.stream().mapToInt(StockItem::quantity).sum();
        return units;
    }

    public String formatCurrency(BigDecimal amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        format.setCurrency(CURRENCY);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(amount.setScale(2, RoundingMode.HALF_EVEN));
    }
}
