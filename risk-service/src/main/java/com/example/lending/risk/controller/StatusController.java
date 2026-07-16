package com.example.lending.risk.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * SAME-PATH TRAP: POST /api/v1/status also exists in loan-service. Different behavior.
 */
@RestController
public class StatusController {

    @PostMapping("/api/v1/status")
    public Map<String, Object> status(@RequestBody(required = false) Map<String, Object> body) {
        // risk-service variant: echoes back an assessment-oriented status
        return Map.of("service", "risk-service", "accepted", true, "received", body == null ? Map.of() : body);
    }
}
