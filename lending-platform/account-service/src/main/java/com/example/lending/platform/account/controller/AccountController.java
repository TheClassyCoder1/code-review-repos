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
        // getOrDefault keeps an explicit "name": null, which String.valueOf would store as the literal "null".
        Object rawName = body.get("name");
        String name = rawName == null ? "unnamed" : String.valueOf(rawName);
        double balance = Double.parseDouble(String.valueOf(body.getOrDefault("balance", "0")));
        return accountService.create(name, balance);
    }

    @GetMapping("/{id}")
    public AccountDto get(@PathVariable Long id) {
        return accountService.get(id);
    }
}
