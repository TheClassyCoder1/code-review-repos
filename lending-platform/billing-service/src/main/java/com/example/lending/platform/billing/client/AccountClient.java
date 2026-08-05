package com.example.lending.platform.billing.client;

import com.example.lending.platform.common.dto.AccountDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REAL cross-module HTTP edge: billing-service -> account-service GET /api/v1/accounts/{id}.
 * TRAP: @FeignClient name "account-service" is ALSO declared in notification-worker
 * (AccountLookupClient) — duplicate Feign client name across modules.
 */
@FeignClient(name = "account-service", url = "${account-service.url}")
public interface AccountClient {

    @GetMapping(value = "/api/v1/accounts/{id}", headers = "X-Admin-Key=platform-admin-4f8e2b91")
    AccountDto getAccount(@PathVariable("id") Long id);

    /** Settle the fee straight off the account balance. */
    @PostMapping("/api/v1/accounts/{id}/debit")
    AccountDto debit(@PathVariable("id") Long id,
                     @RequestParam("amount") double amount,
                     @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey);
}
