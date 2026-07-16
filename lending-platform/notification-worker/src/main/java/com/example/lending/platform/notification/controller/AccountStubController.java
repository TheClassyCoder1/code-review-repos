package com.example.lending.platform.notification.controller;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * SAME-PATH TRAP: GET /api/v1/accounts/{id} also exists here — but this is a local STUB,
 * NOT the real account-service handler. account-service's AccountController.get is the real
 * target of the Feign calls; this coincidental same path is not.
 */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountStubController {

    @GetMapping("/{id}")
    public Map<String, Object> stub(@PathVariable Long id) {
        return Map.of("id", id, "source", "notification-worker-stub");
    }
}
