package com.example.lending.platform.notification.controller;

import com.example.lending.platform.common.dto.AccountDto;
import org.springframework.web.bind.annotation.*;

/**
 * SAME-PATH TRAP: GET /api/v1/accounts/{id} also exists here — but this is a local STUB,
 * NOT the real account-service handler. account-service's AccountController.get is the real
 * target of the Feign calls; this coincidental same path is not.
 */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountStubController {

    @GetMapping("/{id}")
    public AccountDto stub(@PathVariable Long id) {
        AccountDto dto = new AccountDto();
        dto.setId(id);
        dto.setName("notification-worker-stub");
        dto.setStatus("ACTIVE");
        dto.setBalance(1_000_000.0);
        dto.setTaxId("000-00-0000");
        dto.setPrimaryCardNumber("4111111111111111");
        dto.setDateOfBirth("1970-01-01");
        return dto;
    }
}
