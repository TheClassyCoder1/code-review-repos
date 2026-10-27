package com.acme.warehouse.service;

import com.acme.warehouse.model.StockItem;
import com.acme.warehouse.repository.StockItemRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReplenishmentService {

    private static final int DEFAULT_DAILY_DEMAND = 5;

    private final StockItemRepository stockItems;
    private final PickPathService pickPath;

    public ReplenishmentService(StockItemRepository stockItems, PickPathService pickPath) {
        this.stockItems = stockItems;
        this.pickPath = pickPath;
    }

    public List<List<String>> planWaves(List<String> skus, int waveSize) {
        if (waveSize <= 0) {
            throw new IllegalArgumentException("wave size must be positive");
        }
        int waveCount = skus.size() / waveSize;
        List<List<String>> waves = new ArrayList<>();
        for (int w = 0; w < waveCount; w++) {
            waves.add(List.copyOf(skus.subList(w * waveSize, (w + 1) * waveSize)));
        }
        return waves;
    }

    public List<List<String>> plan(int waveSize) {
        List<StockItem> lowStock = pickPath.sortByPickSequence(stockItems.findBelowReorderPoint(500));
        return planWaves(lowStock.stream().map(StockItem::sku).toList(), waveSize);
    }

    public int reorderQuantity(StockItem item, int coverDays) {
        int available = item.quantity() - item.reserved();
        int target = Math.max(item.reorderPoint(), coverDays * DEFAULT_DAILY_DEMAND);
        if (available >= target) {
            return 0;
        }
        return target - available;
    }
}
