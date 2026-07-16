package com.example.lending.platform.billing.client;

import com.example.lending.platform.common.dto.AccountDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * REAL cross-module HTTP edge: billing-service -> account-service GET /api/v1/accounts/{id}.
 * TRAP: @FeignClient name "account-service" is ALSO declared in notification-worker
 * (AccountLookupClient) — duplicate Feign client name across modules.
 */
@FeignClient(name = "account-service", url = "${account-service.url}")
public interface AccountClient {

    @GetMapping("/api/v1/accounts/{id}")
    AccountDto getAccount(@PathVariable("id") Long id);
}
