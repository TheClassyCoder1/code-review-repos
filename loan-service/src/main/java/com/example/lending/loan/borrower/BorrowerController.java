package com.example.lending.loan.borrower;

import com.example.lending.loan.banklink.BankLinkService;
import org.springframework.web.bind.annotation.*;

/** Self-service endpoints for the signed-in borrower. */
@RestController
@RequestMapping("/api/v1/me")
public class BorrowerController {

    private final BorrowerProfileService profileService;
    private final NotificationPreferenceService preferenceService;
    private final BankLinkService bankLinkService;

    public BorrowerController(BorrowerProfileService profileService,
                              NotificationPreferenceService preferenceService,
                              BankLinkService bankLinkService) {
        this.profileService = profileService;
        this.preferenceService = preferenceService;
        this.bankLinkService = bankLinkService;
    }

    @GetMapping("/profile")
    public BorrowerProfile profile(@RequestAttribute(SessionTokenFilter.BORROWER_ID) Long borrowerId) {
        return profileService.profileFor(borrowerId);
    }

    @PutMapping("/profile")
    public BorrowerProfile replaceProfile(@RequestAttribute(SessionTokenFilter.BORROWER_ID) Long borrowerId,
                                          @RequestBody BorrowerProfile profile) {
        return profileService.replaceProfile(borrowerId, profile);
    }

    @PutMapping("/notification-preferences")
    public NotificationPreference updatePreferences(@RequestAttribute(SessionTokenFilter.BORROWER_ID) Long borrowerId,
                                                    @RequestBody PreferenceUpdateRequest request) {
        return preferenceService.update(borrowerId, request);
    }

    @PostMapping("/bank-link")
    public BankLinkService.BankLinkStart startBankLink(@RequestAttribute(SessionTokenFilter.BORROWER_ID) Long borrowerId,
                                       @RequestBody BankLinkRequest request) {
        return bankLinkService.start(borrowerId, request.returnUrl());
    }

    public record BankLinkRequest(String returnUrl) {
    }
}
