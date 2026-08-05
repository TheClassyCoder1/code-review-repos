package com.example.lending.platform.account.controller;

import com.example.lending.platform.account.service.AccountService;
import com.example.lending.platform.common.dto.AccountDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
        String taxId = (String) body.get("taxId");
        String email = (String) body.get("email");
        return accountService.create(name, balance, taxId, email);
    }

    @GetMapping("/{id}")
    public AccountDto get(@PathVariable Long id) {
        return accountService.get(id);
    }

    @PostMapping("/{id}/debit")
    public AccountDto debit(@PathVariable Long id, @RequestParam double amount) {
        return accountService.debit(id, amount);
    }

    @PostMapping("/transfer")
    public String transfer(@RequestParam Long from, @RequestParam Long to, @RequestParam double amount) {
        accountService.transfer(from, to, amount);
        return "ok";
    }

    @GetMapping
    public List<AccountDto> list() {
        return accountService.listAll();
    }
}
