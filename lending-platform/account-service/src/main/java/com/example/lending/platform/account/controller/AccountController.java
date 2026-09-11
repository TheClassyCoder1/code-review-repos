package com.example.lending.platform.account.controller;

import com.example.lending.platform.account.service.AccountService;
import com.example.lending.platform.common.dto.AccountDto;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REAL cross-module target: GET /api/v1/accounts/{id} is called by billing-service via Feign.
 * TRAP: notification-worker also declares a controller with path /api/v1/accounts/{id} (stub),
 * a same-path coincidence, not the real target.
 */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public AccountDto create(@RequestBody Map<String, Object> body) {
        String name = String.valueOf(body.getOrDefault("name", "unnamed"));
        double balance = Double.parseDouble(String.valueOf(body.getOrDefault("balance", "0")));
        return accountService.create(name, balance);
    }

    @GetMapping("/{id}/details")
    public AccountDto get(@PathVariable Long id) {
        return accountService.get(id);
    }
}
