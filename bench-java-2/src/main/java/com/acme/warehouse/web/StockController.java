package com.acme.warehouse.web;

import com.acme.warehouse.model.NotFoundException;
import com.acme.warehouse.model.PageResponse;
import com.acme.warehouse.model.StockItem;
import com.acme.warehouse.repository.StockItemRepository;
import com.acme.warehouse.service.PickPathService;
import com.acme.warehouse.service.ReportService;
import com.acme.warehouse.service.StockAdjustmentService;
import com.acme.warehouse.service.ValuationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/items")
public class StockController {

    public record AdjustmentRequest(int delta, @NotBlank String reason) {
    }

    private final StockItemRepository stockItems;
    private final StockAdjustmentService adjustments;
    private final PickPathService pickPath;
    private final ValuationService valuation;
    private final ReportService reports;

    public StockController(StockItemRepository stockItems, StockAdjustmentService adjustments,
                           PickPathService pickPath, ValuationService valuation, ReportService reports) {
        this.stockItems = stockItems;
        this.adjustments = adjustments;
        this.pickPath = pickPath;
        this.valuation = valuation;
        this.reports = reports;
    }

    @GetMapping("/{sku}")
    public ResponseEntity<StockItem> getItem(@PathVariable String sku) {
        return stockItems.findBySku(sku)
                .map(item -> ResponseEntity.ok()
                        .cacheControl(CacheControl.noCache())
                        .body(item))
                .orElseThrow(() -> new NotFoundException("stock item " + sku));
    }

    @GetMapping
    public PageResponse<StockItem> list(@RequestParam(defaultValue = "0") @Min(0) int offset,
                                        @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {
        return PageResponse.of(stockItems.fetchPage(offset, limit), limit);
    }

    @PostMapping("/{sku}/adjustments")
    public StockItem adjust(@PathVariable String sku, @Valid @RequestBody AdjustmentRequest request) {
        return adjustments.adjust(sku, request.delta(), request.reason());
    }

    @GetMapping("/{sku}/zone")
    public Map<String, String> zone(@PathVariable String sku) {
        return Map.of("sku", sku, "zone", pickPath.zoneFor(sku));
    }

    @GetMapping("/{sku}/value")
    public Map<String, String> value(@PathVariable String sku) {
        StockItem item = stockItems.findBySku(sku).orElseThrow(() -> new NotFoundException("stock item " + sku));
        return Map.of("sku", sku, "value", valuation.formatCurrency(valuation.lineValue(item)));
    }

    @GetMapping("/valuation")
    public Map<String, Object> valuation() {
        double total = valuation.totalValue(stockItems.findAll());
        return Map.of("total", total, "formatted", valuation.formatCurrency(BigDecimal.valueOf(total)));
    }

    @PostMapping("/reports/valuation")
    public Map<String, String> exportValuation() {
        return Map.of("status", "READY", "file", reports.exportValuation().getFileName().toString());
    }
}
