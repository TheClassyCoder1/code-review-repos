package com.example.lending.loan.notify.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/** Forwards servicing notification events to the partner's configured webhook. */
@Component
public class WebhookSink {

    private static final Logger log = LoggerFactory.getLogger(WebhookSink.class);

    private final String webhookUrl;

    public WebhookSink(@Value("${notify.webhook.url}") String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public void put(List<NotificationEvent> events) {
        for (NotificationEvent event : events) {
            try {
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(webhookUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.getOutputStream().write(event.getData() != null ? event.getData() : new byte[0]);
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) {
                log.warn("notification webhook sink: {}", e.toString());
            }
        }
    }
}
