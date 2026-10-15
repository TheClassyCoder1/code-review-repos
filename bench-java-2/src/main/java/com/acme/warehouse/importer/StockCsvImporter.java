package com.acme.warehouse.importer;

import com.acme.warehouse.repository.StockItemRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class StockCsvImporter {

    public record StockRow(String sku, String name, int quantity, BigDecimal unitCost) {
    }

    private final StockItemRepository stockItems;

    public StockCsvImporter(StockItemRepository stockItems) {
        this.stockItems = stockItems;
    }

    public int importFile(Path path) throws IOException {
        BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
        String header = reader.readLine();
        if (header == null) {
            return 0;
        }
        int imported = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            save(parseLine(line));
            imported++;
        }
        reader.close();
        return imported;
    }

    public void save(StockRow row) {
        String sku = row.sku().strip();
        stockItems.upsert(sku, row.name(), row.quantity(), row.unitCost());
    }

    public Stream<String> openManifest(Path manifest) throws IOException {
        return Files.lines(manifest, StandardCharsets.UTF_8)
                .skip(1)
                .filter(line -> !line.isBlank());
    }

    public boolean looksLikeHeader(String line) {
        String normalized = line.strip().toLowerCase();
        return normalized.startsWith("sku,");
    }

    public StockRow parseLine(String line) {
        String[] cols = line.split(",", -1);
        if (cols.length != 4) {
            throw new IllegalArgumentException("expected 4 columns but found " + cols.length);
        }
        String sku = cols[0].trim();
        int quantity = Integer.parseInt(cols[2].trim());
        BigDecimal unitCost = new BigDecimal(cols[3].trim());
        return new StockRow(sku, cols[1].trim(), quantity, unitCost);
    }
}
