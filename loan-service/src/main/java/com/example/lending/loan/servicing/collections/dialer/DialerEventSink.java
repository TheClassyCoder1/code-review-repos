package com.example.lending.loan.servicing.collections.dialer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/** Posts collections events to the outbound dialer platform. */
@Component
public class DialerEventSink {

    private static final Logger log = LoggerFactory.getLogger(DialerEventSink.class);

    private final String url;

    public DialerEventSink(@Value("${servicing.collections.dialer.url}") String url) {
        this.url = url;
    }

    public void put(List<CollectionEvent> events) {
        for (CollectionEvent event : events) {
            try {
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) java.net.URI.create(url).toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/octet-stream");
                conn.getOutputStream().write(event.getData() != null ? event.getData().toBytes() : new byte[0]);
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) {
                log.warn("dialer sink: {}", e.toString());
            }
        }
    }
}
