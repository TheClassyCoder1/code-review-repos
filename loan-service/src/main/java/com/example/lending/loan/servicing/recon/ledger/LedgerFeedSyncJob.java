package com.example.lending.loan.servicing.recon.ledger;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically synchronises every enabled partner ledger feed into the matcher topic. */
@Component
public class LedgerFeedSyncJob {

    private static final Logger log = LoggerFactory.getLogger(LedgerFeedSyncJob.class);
    private static final String TOPIC = "servicing.recon.ledger-entries";

    private final LedgerFeedRepository feedRepository;
    private final CloseableHttpClient httpClient;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public LedgerFeedSyncJob(LedgerFeedRepository feedRepository,
                             @Qualifier("servicingHttpClient") CloseableHttpClient httpClient,
                             @Qualifier("brokerATemplate") KafkaTemplate<String, String> kafkaTemplate) {
        this.feedRepository = feedRepository;
        this.httpClient = httpClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${servicing.recon.ledger-feeds.interval-ms:300000}")
    public void syncAll() {
        for (LedgerFeed feed : feedRepository.findByEnabledTrue()) {
            String key = String.valueOf(feed.getId());
            try {
                new LedgerCtagSync(feed.getPath(), feed, httpClient, HttpClientContext.create(),
                        entry -> kafkaTemplate.send(TOPIC, key, entry)).syncItems();
                feedRepository.save(feed);
            } catch (Exception e) {
                log.warn("Ledger feed {} sync failed: {}", feed.getId(), e.toString());
            }
        }
    }
}
