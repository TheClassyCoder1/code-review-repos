package com.acme.warehouse.service;

import com.acme.warehouse.model.Bin;
import com.acme.warehouse.model.NotFoundException;
import com.acme.warehouse.model.StockItem;
import com.acme.warehouse.repository.BinRepository;
import com.acme.warehouse.repository.StockItemRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PickPathService {

    private final BinRepository bins;
    private final StockItemRepository stockItems;

    public PickPathService(BinRepository bins, StockItemRepository stockItems) {
        this.bins = bins;
        this.stockItems = stockItems;
    }

    public String zoneFor(String sku) {
        StockItem item = stockItems.findBySku(sku)
                .orElseThrow(() -> new NotFoundException("stock item " + sku));
        Map<String, Bin> byCode = bins.findAll().stream()
                .collect(Collectors.toMap(Bin::code, Function.identity()));
        Bin bin = byCode.get(item.binCode());
        return bin.zone();
    }

    public int binsInZone(String zone) {
        List<Bin> zoneBins = bins.findByZone(zone);
        return zoneBins.size();
    }

    public List<StockItem> sortByPickSequence(List<StockItem> items) {
        Map<String, Integer> sequence = new HashMap<>();
        for (Bin bin : bins.findAll()) {
            sequence.put(bin.code(), bin.pickSequence());
        }
        return items.stream()
                .sorted(Comparator.comparingInt((StockItem i) -> sequence.getOrDefault(i.binCode(), Integer.MAX_VALUE))
                        .thenComparing(StockItem::sku))
                .toList();
    }
}
