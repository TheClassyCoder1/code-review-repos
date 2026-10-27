package com.example.lending.loan.ops.tenants;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ops/tenants")
public class TenantEndpoint {

    private final TenantApiKeyService tenantService;

    public TenantEndpoint(TenantApiKeyService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping("/{tenantId}/apikeys/validate")
    public ResponseEntity<Void> validateApiKey(@PathVariable("tenantId") String tenantId,
                                               @RequestParam("key") String apiKey,
                                               @RequestParam("type") TenantApiKeyService.ApiKeyType type) {
        boolean isValid = tenantService.validateApiKeyWithType(tenantId, apiKey, type);
        if (isValid) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
