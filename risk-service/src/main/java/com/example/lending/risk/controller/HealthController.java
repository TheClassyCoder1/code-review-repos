package com.example.lending.risk.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * SAME-PATH TRAP: GET /api/v1/health also exists in loan-service and portal-bff.
 * Identical path, three unrelated handlers. A tool should NOT link them as one endpoint.
 */
@RestController
public class HealthController {

    @GetMapping("/api/v1/health")
    public Map<String, String> health() {
        return Map.of("service", "risk-service", "status", "UP");
    }
}
