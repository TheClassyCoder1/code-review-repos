package com.example.lending.loan.part6.kyc;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/borrowers")
public class BorrowerController {

    private final BorrowerKycService kycService;
    private final BorrowerProfileService profileService;

    public BorrowerController(BorrowerKycService kycService, BorrowerProfileService profileService) {
        this.kycService = kycService;
        this.profileService = profileService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BorrowerView register(@RequestBody BorrowerRegistrationRequest request) {
        return kycService.register(request);
    }

    @GetMapping("/{userId}")
    public BorrowerView get(@PathVariable Long userId) {
        return kycService.get(userId);
    }

    @PutMapping("/{userId}/contact")
    public BorrowerView updateContact(@PathVariable Long userId,
                                      @RequestBody BorrowerRegistrationRequest request) {
        return profileService.updateContact(userId, request);
    }

    @PostMapping("/{userId}/verify")
    public BorrowerView verify(@PathVariable Long userId) {
        return kycService.markVerified(userId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badRequest(IllegalArgumentException e) {
        return e.getMessage();
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String conflict(IllegalStateException e) {
        return e.getMessage();
    }
}
