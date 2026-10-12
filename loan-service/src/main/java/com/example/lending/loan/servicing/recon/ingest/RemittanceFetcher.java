package com.example.lending.loan.servicing.recon.ingest;

import com.example.lending.loan.servicing.recon.ingest.RemittanceBatchSource.RemittanceBatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/** Feeds batches from a remittance file into the merger. */
public class RemittanceFetcher implements Runnable {

    /** Busy/idle gauge of remittance fetcher threads. */
    public static class RemittanceFetchMetrics {

        private final AtomicInteger busyThreads = new AtomicInteger();

        public void threadBusy() {
            busyThreads.incrementAndGet();
        }

        public void threadFree() {
            busyThreads.decrementAndGet();
        }

        public int busyThreads() {
            return busyThreads.get();
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(RemittanceFetcher.class);

    private final RemittanceBatchSource source;
    private final RemittanceMerger merger;
    private final RemittanceFetchMetrics metrics;
    private volatile boolean stopped = false;

    public RemittanceFetcher(RemittanceBatchSource source, RemittanceMerger merger, RemittanceFetchMetrics metrics) {
        this.source = source;
        this.merger = merger;
        this.metrics = metrics;
    }

    @Override
    public void run() {
        try {
            fetchAndMerge();
        } finally {
            merger.close();
        }
    }

    public void fetchAndMerge() {
        while (!stopped) {
            try {
                // Wait until the merger can take another batch
                merger.waitForResource();
                if (metrics != null) {
                    metrics.threadBusy();
                }
                fetchToLocalAndMerge();
            } catch (Exception e) {
                logger.error("Remittance fetcher fetch data failed.", e);
            } finally {
                if (metrics != null) {
                    metrics.threadFree();
                }
            }
        }
    }

    private void fetchToLocalAndMerge() throws IOException, InterruptedException {
        RemittanceBatch batch = source.next();
        if (batch == null) {
            return;
        }
        merger.merge(batch);
    }

    public void stopFetch() {
        stopped = true;
    }
}
