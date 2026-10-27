package com.example.lending.loan.part6.kyc;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * KYC endpoints called by portal-bff KycClient:
 *  - POST /api/v1/kyc
 *  - GET  /api/v1/kyc/{userId}
 */
@RestController
@RequestMapping("/api/v1/kyc")
public class KycController {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public KycStatusDto submit(@RequestBody KycSubmissionRequest request) {
        return kycService.submit(request);
    }

    @GetMapping("/{userId}")
    public KycStatusDto status(@PathVariable Long userId) {
        return kycService.status(userId);
    }
}
