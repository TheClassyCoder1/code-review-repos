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

    public LoanCallbackClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public LoanDto fetchLoan(Long id) {
        return restTemplate.getForObject(
                baseUrl + "/api/v1/loans/{id}", LoanDto.class, id);
    }
}
