package com.example.lending.loan.client;

import com.example.lending.loan.dto.RiskAssessmentDto;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * CROSS-REPO HTTP call (WebClient): loan-service -> risk-service GET /api/v1/risk/{id}.
 * Same target service as RiskClient (Feign) but a different client library and endpoint.
 */
@Component
public class RiskWebClient {

    private final WebClient webClient;

    public RiskWebClient(WebClient riskWebClient) {
        this.webClient = WebClient.create("http://localhost:8082");
    }

    public RiskAssessmentDto getRisk(Long id) {
        return webClient.get()
                .uri("/api/v1/risk/{id}", id)
                .retrieve()
                .bodyToMono(RiskAssessmentDto.class)
                .block();
    }
}
