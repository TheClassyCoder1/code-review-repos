package com.example.lending.loan.servicing.recon.ingest;

import com.example.lending.loan.servicing.recon.ingest.RemittanceBatchSource.RemittanceBatch;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

/** Aggregates remittance lines per loan reference. Limits the number of batches held in memory. */
public class RemittanceMerger {

    private final Semaphore permits;
    private final Map<String, AtomicLong> totalsByLoan = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    public RemittanceMerger(int maxBatchesInFlight) {
        this.permits = new Semaphore(maxBatchesInFlight);
    }

    /** Blocks while the merger already holds the maximum number of batches. */
    public void waitForResource() throws InterruptedException {
        permits.acquire();
        permits.release();
    }

    public void merge(RemittanceBatch batch) throws InterruptedException {
        permits.acquire();
        try {
            for (RemittanceBatchSource.RemittanceLine line : batch.lines()) {
                totalsByLoan.computeIfAbsent(line.loanReference(), k -> new AtomicLong()).addAndGet(line.amountMinor());
            }
        } finally {
            permits.release();
        }
    }

    public Map<String, Long> totals() {
        Map<String, Long> copy = new ConcurrentHashMap<>();
        totalsByLoan.forEach((k, v) -> copy.put(k, v.get()));
        return copy;
    }

    public void close() {
        closed.set(true);
    }

    public boolean isClosed() {
        return closed.get();
    }
}
