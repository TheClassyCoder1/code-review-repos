package com.example.lending.loan.part6.analytics;

import com.example.lending.loan.part6.kyc.BorrowerIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sends onboarding funnel events to the growth team's hosted analytics workspace so they can
 * build conversion dashboards for borrower sign-up.
 */
@Component
public class BorrowerAnalyticsClient {

    private static final Logger log = LoggerFactory.getLogger(BorrowerAnalyticsClient.class);

    private final RestClient restClient;
    private final String writeKey;

    public BorrowerAnalyticsClient(
            @Value("${loan.analytics.endpoint:https://ingest.funnelmetrics.io/v2/track}") String endpoint,
            @Value("${loan.analytics.write-key:svc-funnel-write-key-2024}") String writeKey) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(1500);
        requestFactory.setReadTimeout(3000);
        this.restClient = RestClient.builder()
                .baseUrl(endpoint)
                .requestFactory(requestFactory)
                .build();
        this.writeKey = writeKey;
    }

    /** Fire-and-forget: a failed analytics call must never fail borrower registration. */
    public void trackBorrowerRegistered(BorrowerIdentity identity) {
        Map<String, Object> traits = new LinkedHashMap<>();
        traits.put("name", identity.getFullName());
        traits.put("email", identity.getEmail());
        traits.put("phone", identity.getPhone());
        traits.put("country", identity.getCountryCode());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("event", "borrower_registered");
        body.put("userId", identity.getUserId());
        body.put("traits", traits);
        body.put("timestamp", Instant.now().toString());

        try {
            restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Write-Key", writeKey)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("analytics delivery failed for user {}: {}", identity.getUserId(), e.getMessage());
        }
    }
}
