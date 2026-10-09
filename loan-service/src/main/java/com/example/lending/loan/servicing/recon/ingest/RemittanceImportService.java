package com.example.lending.loan.servicing.recon.ingest;

import com.example.lending.loan.servicing.recon.ingest.RemittanceFetcher.RemittanceFetchMetrics;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Runs remittance advice imports in the background and exposes their progress. */
@Service
public class RemittanceImportService {

    private static final int BATCH_SIZE = 500;
    private static final int MAX_BATCHES_IN_FLIGHT = 4;

    private final Path inboundDirectory;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final RemittanceFetchMetrics metrics = new RemittanceFetchMetrics();
    private final Map<String, RemittanceMerger> imports = new ConcurrentHashMap<>();

    public RemittanceImportService(@Value("${servicing.recon.remittance.inbound-dir}") String inboundDirectory) {
        this.inboundDirectory = Path.of(inboundDirectory).toAbsolutePath().normalize();
    }

    public String startImport(String fileName) throws IOException {
        Path file = inboundDirectory.resolve(fileName).normalize();
        if (!file.startsWith(inboundDirectory) || !Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Unknown remittance file");
        }
        String importId = UUID.randomUUID().toString();
        RemittanceMerger merger = new RemittanceMerger(MAX_BATCHES_IN_FLIGHT);
        RemittanceBatchSource source = new RemittanceBatchSource(file, BATCH_SIZE);
        imports.put(importId, merger);
        executor.submit(() -> {
            try (source) {
                new RemittanceFetcher(source, merger, metrics).run();
            } catch (IOException e) {
                merger.close();
            }
        });
        return importId;
    }

    public Map<String, Object> status(String importId) {
        RemittanceMerger merger = imports.get(importId);
        if (merger == null) {
            return Map.of("state", "UNKNOWN");
        }
        if (!merger.isClosed()) {
            return Map.of("state", "RUNNING", "busyThreads", metrics.busyThreads());
        }
        return Map.of("state", "DONE", "loans", merger.totals().size());
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
