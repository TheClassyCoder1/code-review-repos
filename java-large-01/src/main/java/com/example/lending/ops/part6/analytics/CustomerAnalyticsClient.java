package com.example.lending.ops.part6.analytics;

import com.example.lending.ops.part6.outreach.CustomerContact;
import lombok.extern.slf4j.Slf4j;
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
 * Pushes customer profiles to the marketing CDP so collections and retention campaigns can be
 * segmented by the vendor.
 */
@Slf4j
@Component
public class CustomerAnalyticsClient {

    private final RestClient restClient;

    public CustomerAnalyticsClient(@Value("${ops.analytics.cdp-url:https://api.segmently-cdp.example/v1}") String baseUrl,
                                   @Value("${ops.analytics.cdp-key:}") String apiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public void identify(CustomerContact contact, String segment) {
        Map<String, Object> traits = new LinkedHashMap<>();
        traits.put("email", contact.getEmail());
        traits.put("phone", contact.getPhone());
        traits.put("name", contact.getFullName());
        traits.put("birthday", contact.getDateOfBirth() == null ? null : contact.getDateOfBirth().toString());
        traits.put("segment", segment);
        traits.put("language", contact.getPreferredLanguage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", String.valueOf(contact.getCustomerId()));
        body.put("traits", traits);
        body.put("timestamp", Instant.now().toString());
        try {
            restClient.post().uri("/identify").contentType(MediaType.APPLICATION_JSON).body(body)
                    .retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("cdp identify failed for customer {}: {}", contact.getCustomerId(), e.getMessage());
        }
    }
}
