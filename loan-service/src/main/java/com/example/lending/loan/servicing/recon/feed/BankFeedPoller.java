package com.example.lending.loan.servicing.recon.feed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

/** Pulls the settlement bank's statement feed and hands every line to the matcher topic. */
@Component
public class BankFeedPoller {

    private static final Logger log = LoggerFactory.getLogger(BankFeedPoller.class);
    private static final String TOPIC = "servicing.recon.bank-lines";
    private static final int MAX_LINES = 50_000;

    private final String feedUrl;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public BankFeedPoller(@Value("${servicing.recon.bank-feed-url}") String feedUrl,
                          @Qualifier("brokerATemplate") KafkaTemplate<String, String> kafkaTemplate) {
        this.feedUrl = feedUrl;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(cron = "${servicing.recon.bank-feed-cron:0 */15 * * * *}")
    public void poll() {
        try {
            HttpURLConnection conn = BankFeedConnector.getFeedConnection(feedUrl);
            int lines = 0;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null && lines < MAX_LINES) {
                    if (!line.isBlank() && !line.startsWith("#")) {
                        kafkaTemplate.send(TOPIC, line);
                        lines++;
                    }
                }
            } finally {
                conn.disconnect();
            }
            log.info("Bank feed: forwarded {} lines", lines);
        } catch (Exception e) {
            log.warn("Bank feed poll failed: {}", e.toString());
        }
    }
}
