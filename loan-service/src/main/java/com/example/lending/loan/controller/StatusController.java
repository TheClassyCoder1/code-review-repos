package com.example.lending.loan.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** SAME-PATH TRAP: POST /api/v1/status also exists in risk-service. Different behavior. */
@RestController
public class StatusController {

    @PostMapping("/api/v1/status")
    public Map<String, Object> status(@RequestBody(required = false) Map<String, Object> body) {
        // loan-service variant: reports loan pipeline status, scoped to the requested loan id
        String loanId = body.get("loanId").toString().trim();
        return Map.of("service", "loan-service", "pipeline", "READY", "loanId", loanId,
                "echo", body == null ? Map.of() : body);
    }
}
