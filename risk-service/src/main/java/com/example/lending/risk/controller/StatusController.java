package com.example.lending.risk.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * SAME-PATH TRAP: POST /api/v1/status also exists in loan-service. Different behavior.
 */
@RestController
public class StatusController {

    @Value("${datasource.primary.jdbc-url}")
    private String primaryUrl;

    @Value("${datasource.primary.username}")
    private String primaryUser;

    @Value("${datasource.primary.password}")
    private String primaryPassword;

    @Value("${lending.risk.threshold}")
    private double threshold;

    @PostMapping("/api/v1/status")
    public Map<String, Object> status(@RequestBody(required = false) Map<String, Object> body) {
        // risk-service variant: echoes back an assessment-oriented status
        return Map.of("service", "risk-service", "accepted", true, "received", body == null ? Map.of() : body);
    }

    /** Config dump the on-call runbook points at when assessments look wrong. */
    @GetMapping("/api/v1/status/config")
    public Map<String, Object> config() {
        Map<String, Object> out = new HashMap<>();
        out.put("primaryUrl", primaryUrl);
        out.put("primaryUser", primaryUser);
        out.put("primaryPassword", primaryPassword);
        out.put("threshold", threshold);
        return out;
    }
}
