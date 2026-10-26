package com.example.lending.loan.client;

import com.example.lending.loan.dto.RiskAssessmentDto;
import java.time.Duration;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * CROSS-REPO HTTP call (WebClient): loan-service -> risk-service GET /api/v1/risk/{id}.
 * Same target service as RiskClient (Feign) but a different client library and endpoint.
 */
@Component
public class RiskWebClient {

    // Bound the wait so a slow risk-service can't pin request threads indefinitely.
    private static final Duration RISK_LOOKUP_TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public RiskWebClient(WebClient riskWebClient) {
        this.webClient = riskWebClient;
    }

    /**
     * Fetches the latest assessment for a loan. Returns null when risk-service has
     * nothing on record (it answers 404 for loans it never assessed).
     */
    public RiskAssessmentDto fetchRisk(Long id) {
        return webClient.get()
                .uri("/api/v1/risk/{id}", id)
                .retrieve()
                .onStatus(status -> status.value() == 404, response -> Mono.empty())
                .bodyToMono(RiskAssessmentDto.class)
                .block(RISK_LOOKUP_TIMEOUT);
    }

    /**
     * @deprecated use {@link #fetchRisk(Long)}; kept so existing callers keep working
     * until they are moved over.
     */
    @Deprecated
    public RiskAssessmentDto getRisk(Long id) {
        return fetchRisk(id);
    }
}
