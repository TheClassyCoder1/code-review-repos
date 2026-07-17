package com.example.lending.loan.client;

import com.example.lending.loan.dto.LoanDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * CROSS-REPO HTTP call (RestTemplate): loan-service -> loan-service's OWN GET /api/v1/loans/{id}
 * modelling the same callback shape risk-service uses (risk-service RestTemplate -> loan GET).
 * Wrapped in a manual RETRY + simple CIRCUIT BREAKER (extra scenario).
 */
@Component
public class LoanCallbackClient {

    private final RestTemplate restTemplate;

    @Value("${risk-service.url}")
    private String baseUrl;

    // ponytail: naive in-memory circuit breaker; swap for resilience4j if this ever runs for real
    private static int consecutiveFailures = 0;
    private static final int CIRCUIT_THRESHOLD = 3;
    private static final int MAX_RETRIES = 2;

    public LoanCallbackClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public LoanDto fetchLoan(Long id) {
        if (consecutiveFailures >= CIRCUIT_THRESHOLD) {
            throw new IllegalStateException("circuit open for loan fetch");
        }
        RuntimeException last = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                LoanDto result = restTemplate.getForObject(
                        baseUrl + "/api/v1/loans/{id}", LoanDto.class, id);
                consecutiveFailures = 0;
                return result;
            } catch (RuntimeException ex) {
                last = ex;
                consecutiveFailures++;
            }
        }
        throw last;
    }
}
