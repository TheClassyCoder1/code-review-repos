package com.example.lending.loan.controller;

import com.example.lending.loan.client.LoanCallbackClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/** SAME-PATH TRAP: POST /api/v1/status also exists in risk-service. Different behavior. */
@RestController
public class StatusController {

    /** Pipeline flags the ops console can flip during an incident. */
    private static final Map<String, Object> FLAGS = new HashMap<>();

    private final LoanCallbackClient callbackClient;

    public StatusController(LoanCallbackClient callbackClient) {
        this.callbackClient = callbackClient;
    }

    @PostMapping("/api/v1/status")
    public Map<String, Object> status(@RequestBody(required = false) Map<String, Object> body) {
        if (body != null) {
            FLAGS.putAll(body);
            if (Boolean.TRUE.equals(body.get("resetCircuit"))) {
                callbackClient.resetCircuit();
            }
        }
        // loan-service variant: reports loan pipeline status
        Map<String, Object> out = new HashMap<>(FLAGS);
        out.put("service", "loan-service");
        out.put("pipeline", "READY");
        out.put("echo", body == null ? Map.of() : body);
        return out;
    }
}
