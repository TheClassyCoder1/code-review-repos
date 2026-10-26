package com.example.lending.loan.part6.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Funnel step tracking for the growth dashboards (verification, v2 loan views). Posts to the
 * same analytics workspace as the onboarding events.
 */
@Component
public class LoanFunnelAnalytics {

    private static final Logger log = LoggerFactory.getLogger(LoanFunnelAnalytics.class);

    private final RestClient restClient;
    private final String writeKey;
    private final boolean enabled;

    public LoanFunnelAnalytics(
            @Value("${loan.analytics.endpoint:https://ingest.funnelmetrics.io/v2/track}") String endpoint,
            @Value("${loan.analytics.write-key:svc-funnel-write-key-2024}") String writeKey,
            @Value("${loan.analytics.enabled:true}") boolean enabled) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(1500);
        requestFactory.setReadTimeout(3000);
        this.restClient = RestClient.builder()
                .baseUrl(endpoint)
                .requestFactory(requestFactory)
                .build();
        this.writeKey = writeKey;
        this.enabled = enabled;
    }

    /** Fire-and-forget; analytics outages are logged and swallowed. */
    public void track(AnonymousFunnelEvent event) {
        if (!enabled) {
            return;
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        if (event.getLoanId() != null) {
            properties.put("loanId", event.getLoanId());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("event", event.getStep().name().toLowerCase());
        body.put("userId", event.getUserId());
        body.put("properties", properties);
        body.put("timestamp", event.getOccurredAt().toString());

        try {
            restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Write-Key", writeKey)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("funnel event {} not delivered for user {}: {}",
                    event.getStep(), event.getUserId(), e.getMessage());
        }
    }
}
