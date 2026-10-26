package com.example.lending.loan.part6;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Tuning for borrower loan exports, loaded from part6/loan-export.properties.
 */
@Configuration
@PropertySource("classpath:part6/loan-export.properties")
public class LoanExportSettings {

    private final int batchSize;
    private final String delimiter;

    public LoanExportSettings(@Value("${lending.export.batch-size:500}") int batchSize,
                              @Value("${lending.export.delimiter:,}") String delimiter) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("lending export batch size must be positive");
        }
        this.batchSize = batchSize;
        this.delimiter = delimiter;
    }

    public int getBatchSize() { return batchSize; }

    public String getDelimiter() { return delimiter; }
}
