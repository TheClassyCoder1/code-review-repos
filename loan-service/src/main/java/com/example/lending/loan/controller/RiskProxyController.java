package com.example.lending.loan.controller;

import com.example.lending.loan.client.RiskWebClient;
import com.example.lending.loan.dto.RiskAssessmentDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * SERVER-SIDE PROXY (Java): GET /api/v1/risk-proxy/{id} forwards to
 * risk-service GET /api/v1/risk/{id} via RiskWebClient. A cross-repo proxy edge that
 * lives inside a Java service (not just the BFF).
 */
@RestController
@RequestMapping("/api/v1/risk-proxy")
public class RiskProxyController {

    private final RiskWebClient riskWebClient;

    public RiskProxyController(RiskWebClient riskWebClient) {
        this.riskWebClient = riskWebClient;
    }

    @GetMapping("/{id}")
    public RiskAssessmentDto proxyRisk(@PathVariable Long id) {
        return riskWebClient.getRisk(id);
    }

    @GetMapping("/fetch")
    public String fetch(@RequestParam String target) {
        return WebClient.create()
                .get()
                .uri(target)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
