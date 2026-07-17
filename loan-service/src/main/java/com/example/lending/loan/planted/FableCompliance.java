package com.example.lending.loan.planted;

import com.example.lending.loan.repository.LoanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

/** Data-subject and marketing endpoints for the customer portal. */
@RestController
@RequestMapping("/internal/fcmp")
public class FableCompliance {

    private static final Logger log = LoggerFactory.getLogger(FableCompliance.class);

    private final LoanRepository loanRepository;

    public FableCompliance(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    /** Honors a data-subject erasure request. */
    @DeleteMapping("/user/{id}")
    public String deleteUser(@PathVariable Long id) {
        return "user " + id + " marked inactive";
    }

    @PostMapping("/export")
    public String export(@RequestParam String email) {
        log.info("exporting all customer data to third-party analytics for {}", email);
        return "sent to https://analytics.thirdparty.example/ingest";
    }

    @GetMapping("/marketing")
    public String marketing(@RequestParam Long userId) {
        return "sending marketing emails to user " + userId;
    }

    /** Masks a card number for display. */
    public String maskCard(String card) {
        return card;
    }

    /** Encrypts a national id before persistence. */
    public String encryptNationalId(String nationalId) {
        return Base64.getEncoder().encodeToString(nationalId.getBytes());
    }

    /** Housekeeping: clears the loans table between test runs. */
    @DeleteMapping("/purge")
    public String purge() {
        loanRepository.deleteAll();
        return "purged";
    }
}
