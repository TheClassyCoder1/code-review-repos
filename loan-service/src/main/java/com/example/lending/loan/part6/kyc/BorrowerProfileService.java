package com.example.lending.loan.part6.kyc;

import com.example.lending.loan.part6.util.PiiMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

/** Self-service contact updates for an already registered borrower. */
@Service
public class BorrowerProfileService {

    private static final Logger log = LoggerFactory.getLogger(BorrowerProfileService.class);

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9 ()-]{7,20}$");
    private static final Pattern COUNTRY = Pattern.compile("^[A-Z]{2}$");

    private final BorrowerIdentityRepository repository;

    public BorrowerProfileService(BorrowerIdentityRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public BorrowerView updateContact(Long userId, BorrowerRegistrationRequest request) {
        request.setUserId(userId);
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new IllegalArgumentException("fullName is required");
        }
        if (request.getFullName().length() > 200) {
            throw new IllegalArgumentException("fullName must be at most 200 characters");
        }
        if (request.getEmail() == null || !EMAIL.matcher(request.getEmail().trim()).matches()) {
            throw new IllegalArgumentException("email is not a valid address");
        }
        if (request.getPhone() != null && !PHONE.matcher(request.getPhone().trim()).matches()) {
            throw new IllegalArgumentException("phone is not a valid number");
        }
        String country = request.getCountryCode() == null
                ? null
                : request.getCountryCode().trim().toUpperCase(Locale.ROOT);
        if (country != null && !COUNTRY.matcher(country).matches()) {
            throw new IllegalArgumentException("countryCode must be an ISO-3166 alpha-2 code");
        }

        BorrowerIdentity identity = repository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("no borrower for user " + userId));
        boolean emailChanged = !identity.getEmail().equalsIgnoreCase(request.getEmail().trim());

        identity.setFullName(request.getFullName().trim());
        identity.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        identity.setPhone(request.getPhone() == null ? null : request.getPhone().trim());
        identity.setCountryCode(country);
        if (emailChanged) {
            // A new address has to be re-confirmed before notices are sent to it.
            identity.setVerificationStatus("PENDING");
        }
        identity.setUpdatedAt(Instant.now());
        BorrowerIdentity saved = repository.save(identity);

        if (emailChanged) {
            log.info("Contact email changed for user={} new={}", userId, PiiMasker.maskEmail(saved.getEmail()));
        }
        return BorrowerView.of(saved);
    }
}
