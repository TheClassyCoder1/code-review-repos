package com.example.lending.loan.servicing.recon.ingest;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Reads a remittance advice file in fixed-size batches. Returns {@code null} once the file is exhausted. */
public class RemittanceBatchSource implements Closeable {

    /** Slice of a remittance advice file: payer references and amounts in minor units. */
    public record RemittanceBatch(int sequence, List<RemittanceLine> lines) {
    }

    public record RemittanceLine(String payerReference, String loanReference, long amountMinor) {
    }

    private final BufferedReader reader;
    private final int batchSize;
    private int sequence;

    public RemittanceBatchSource(Path file, int batchSize) throws IOException {
        this.reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
        this.batchSize = batchSize;
    }

    public synchronized RemittanceBatch next() throws IOException {
        List<RemittanceLine> lines = new ArrayList<>(batchSize);
        String line;
        while (lines.size() < batchSize && (line = reader.readLine()) != null) {
            String[] fields = line.split(";");
            if (fields.length == 3) {
                lines.add(new RemittanceLine(fields[0].trim(), fields[1].trim(),
                        Long.parseLong(fields[2].trim())));
            }
        }
        if (lines.isEmpty()) {
            return null;
        }
        return new RemittanceBatch(sequence++, lines);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
