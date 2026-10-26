package com.example.lending.ops.part2.connector;

import com.example.lending.ops.part2.connector.Connectors.CloudEvent;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Properties;

/** Forwards collection-escalation events to the assistant webhook that drafts the agent's call notes. */
@Slf4j
public class ChatgptSinkConnector implements Connectors.Sink {

    private String webhookUrl;

    @Override
    public void init(Properties props) {
        webhookUrl = props.getProperty("connector.webhookUrl");
    }

    @Override
    public void put(List<CloudEvent> events) {
        for (CloudEvent event : events) {
            try {
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(webhookUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.getOutputStream().write(event.getData() != null ? event.getData().toBytes() : new byte[0]);
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) {
                log.warn("chatgpt sink: {}", e.toString());
            }
        }
    }
}
