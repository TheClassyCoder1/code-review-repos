package com.example.lending.ops.part6.analytics;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/** HTTP sink for the external funnel-analytics vendor. Every payload goes through the policy first. */
@Slf4j
@Component
public class FunnelAnalyticsSink {

    private final RestClient restClient;
    private final AnalyticsPayloadPolicy policy;

    public FunnelAnalyticsSink(AnalyticsPayloadPolicy policy,
                               @Value("${ops.analytics.funnel-url:https://collect.funnelmetrics.example/v2/events}") String url,
                               @Value("${ops.analytics.funnel-write-key:}") String writeKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.policy = policy;
        this.restClient = RestClient.builder()
                .baseUrl(url)
                .requestFactory(factory)
                .defaultHeader("X-Write-Key", writeKey)
                .build();
    }

    public void send(Map<String, Object> payload) {
        Map<String, Object> checked = policy.requireInternalIdsOnly(payload);
        try {
            restClient.post().contentType(MediaType.APPLICATION_JSON).body(checked).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            // Analytics is best effort; never fail a business flow because the vendor is down.
            log.warn("funnel analytics event {} dropped: {}", checked.get("event"), e.getMessage());
        }
    }
}
