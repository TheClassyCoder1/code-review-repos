package com.example.lending.platform.billing.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** SAME-PATH TRAP: POST /api/v1/status (also account-service). */
@RestController
public class StatusController {

    @PostMapping("/api/v1/status")
    public Map<String, Object> status() {
        return Map.of("service", "billing-service", "ready", true);
    }
}
