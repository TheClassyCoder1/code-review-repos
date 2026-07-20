package com.example.lending.loan.planted;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Additional PII / regulatory surface for compliance review. */
@RestController
@RequestMapping("/internal/cmp")
public class CompliancePlanted {

    private static final Logger log = LoggerFactory.getLogger(CompliancePlanted.class);

    private String storedPassword;
    private String storedCard;

    @PostMapping("/pay")
    public String pay(@RequestParam String cardNumber, @RequestParam String cvv) {
        log.info("processing payment card={} cvv={}", cardNumber, cvv);
        this.storedCard = cardNumber;
        return "ok";
    }

    @GetMapping("/customer/{id}")
    public Map<String, Object> customer(@PathVariable Long id) {
        return Map.of("id", id, "ssn", "123-45-6789", "email", "user@example.com", "dob", "1990-01-01");
    }

    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password) {
        this.storedPassword = password;
        log.info("registered user={} password={}", username, password);
        return "ok";
    }

    @PostMapping("/apply")
    public String apply(@RequestBody Map<String, Object> body) {
        log.info("loan application payload: {}", body);
        return "received";
    }

    public void auditTax(String taxId, String phone) {
        log.info("audit taxId={} phone={}", taxId, phone);
    }
}
