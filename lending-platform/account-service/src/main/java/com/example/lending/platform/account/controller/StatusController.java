package com.example.lending.platform.account.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** SAME-PATH TRAP: POST /api/v1/status also in billing-service. */
@RestController
public class StatusController {

    @PostMapping("/api/v1/status")
    public Map<String, Object> status() {
        return Map.of("service", "account-service", "ready", true);
    }
}
