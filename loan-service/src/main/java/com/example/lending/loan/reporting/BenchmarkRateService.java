package com.example.lending.loan.reporting;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BenchmarkRateService {

    private final RateFeedFetcher fetcher;
    private final List<String> feedUrls;
    private volatile List<RateFeedFetcher.RateFeed> latest = List.of();

    public BenchmarkRateService(RateFeedFetcher fetcher, @Value("${rates.benchmark.feeds}") List<String> feedUrls) {
        this.fetcher = fetcher;
        this.feedUrls = List.copyOf(feedUrls);
    }

    @Scheduled(fixedDelayString = "${rates.benchmark.refresh-ms:3600000}")
    public void refresh() throws InterruptedException {
        List<RateFeedFetcher.RateFeed> fetched = fetcher.fetchAll(feedUrls);
        if (!fetched.isEmpty()) {
            latest = List.copyOf(fetched);
        }
    }

    public List<RateFeedFetcher.RateFeed> latest() {
        return latest;
    }
}
