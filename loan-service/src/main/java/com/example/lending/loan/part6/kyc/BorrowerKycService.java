package com.example.lending.loan.part6.kyc;

import com.example.lending.loan.part6.analytics.AnonymousFunnelEvent;
import com.example.lending.loan.part6.analytics.BorrowerAnalyticsClient;
import com.example.lending.loan.part6.analytics.LoanFunnelAnalytics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Borrower onboarding: captures the identity record that underwriting needs before a loan
 * can move past APPLIED.
 */
@Service
public class BorrowerKycService {

    private static final Logger log = LoggerFactory.getLogger(BorrowerKycService.class);

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9 ()-]{7,20}$");
    private static final Pattern COUNTRY = Pattern.compile("^[A-Z]{2}$");

    private final BorrowerIdentityRepository repository;
    private final BorrowerAnalyticsClient analyticsClient;
    private final LoanFunnelAnalytics funnelAnalytics;

    public BorrowerKycService(BorrowerIdentityRepository repository,
                              BorrowerAnalyticsClient analyticsClient,
                              LoanFunnelAnalytics funnelAnalytics) {
        this.repository = repository;
        this.analyticsClient = analyticsClient;
        this.funnelAnalytics = funnelAnalytics;
    }

    @Transactional
    public BorrowerView register(BorrowerRegistrationRequest request) {
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
        if (request.getNationalId() == null || request.getNationalId().isBlank()) {
            throw new IllegalArgumentException("nationalId is required");
        }
        if (repository.existsByUserId(request.getUserId())) {
            throw new IllegalStateException("borrower already registered for user " + request.getUserId());
        }

        BorrowerIdentity identity = new BorrowerIdentity();
        identity.setUserId(request.getUserId());
        identity.setFullName(request.getFullName().trim());
        identity.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        identity.setPhone(request.getPhone() == null ? null : request.getPhone().trim());
        identity.setNationalId(request.getNationalId().trim());
        identity.setCountryCode(country);
        identity.setVerificationStatus("PENDING");
        identity.setCreatedAt(Instant.now());
        BorrowerIdentity saved = repository.save(identity);

        log.info("Registered borrower user={} email={} phone={}", saved.getUserId(), saved.getEmail(), saved.getPhone());
        analyticsClient.trackBorrowerRegistered(saved);
        return BorrowerView.of(saved);
    }

    @Transactional
    public BorrowerView markVerified(Long userId) {
        BorrowerIdentity identity = repository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("no borrower for user " + userId));
        identity.setVerificationStatus("VERIFIED");
        identity.setUpdatedAt(Instant.now());
        log.info("Borrower user={} verified", userId);
        BorrowerIdentity saved = repository.save(identity);
        funnelAnalytics.track(AnonymousFunnelEvent.forUser(AnonymousFunnelEvent.Step.BORROWER_VERIFIED, saved.getUserId()));
        return BorrowerView.of(saved);
    }

    public BorrowerView get(Long userId) {
        return repository.findByUserId(userId)
                .map(BorrowerView::of)
                .orElseThrow(() -> new IllegalArgumentException("no borrower for user " + userId));
    }
}
