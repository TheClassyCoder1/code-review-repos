package com.example.lending.platform.account.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** SAME-PATH TRAP: GET /api/v1/health in all 3 platform modules + POST /api/v1/status in two. */
@RestController
public class HealthController {

    private static final String STATUS_UP = "UP";

    @GetMapping("/api/v1/health")
    public Map<String, String> health() {
        return Map.of("service", "account-service", "status", STATUS_UP);
    }
}
