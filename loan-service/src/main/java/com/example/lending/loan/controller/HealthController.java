package com.example.lending.loan.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** SAME-PATH TRAP: GET /api/v1/health also exists in risk-service and portal-bff. */
@RestController
public class HealthController {

    @GetMapping("/api/v1/health")
    public Map<String, String> health() {
        return Map.of("service", "loan-service", "status", "UP");
    }
}
