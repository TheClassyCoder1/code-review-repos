package com.acme.warehouse.service;

import com.acme.warehouse.model.StockItem;
import com.acme.warehouse.repository.StockItemRepository;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private final StockItemRepository stockItems;
    private final ValuationService valuation;
    private final Path reportDir;

    public ReportService(StockItemRepository stockItems, ValuationService valuation,
                         @Value("${warehouse.reports.dir}") String reportDir) {
        this.stockItems = stockItems;
        this.valuation = valuation;
        this.reportDir = Path.of(reportDir);
    }

    public Path exportValuation() {
        List<StockItem> items = stockItems.findAll();
        Path target = reportDir.resolve("valuation-" + LocalDate.now() + ".csv");
        CompletableFuture.runAsync(() -> writeValuationCsv(target, items));
        return target;
    }

    private void writeValuationCsv(Path target, List<StockItem> items) {
        try (BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            writer.write("sku,name,quantity,unit_cost,line_value");
            writer.newLine();
            for (StockItem item : items) {
                writer.write(String.join(",", item.sku(), item.name(), String.valueOf(item.quantity()),
                        item.unitCost().toPlainString(), valuation.lineValue(item).toPlainString()));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
