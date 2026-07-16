package com.example.lending.platform.notification.client;

import com.example.lending.platform.common.dto.AccountDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * DUPLICATE @FeignClient NAME TRAP: name "account-service" is ALSO used by billing-service's
 * AccountClient. Same logical target (account-service GET /api/v1/accounts/{id}), declared
 * independently in two modules. Both are real edges to account-service.
 */
@FeignClient(name = "account-service", url = "${account-service.url}")
public interface AccountLookupClient {

    @GetMapping("/api/v1/accounts/{id}")
    AccountDto lookup(@PathVariable("id") Long id);
}
