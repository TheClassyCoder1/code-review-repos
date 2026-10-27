package com.example.lending.loan.part6.kyc;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records onboarding identity data and decides whether a case can be auto-verified or has to
 * go to manual review. A national id already registered to another user always goes to review.
 */
@Service
public class KycService {

    private static final Logger log = LoggerFactory.getLogger(KycService.class);
    private static final int MINIMUM_AGE_YEARS = 18;

    private final KycProfileRepository repository;
    private final Clock clock;

    public KycService(KycProfileRepository repository) {
        this(repository, Clock.systemUTC());
    }

    KycService(KycProfileRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public KycStatusDto submit(KycSubmissionRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("kyc submission is missing a userId");
        }
        if (request.getCountry() == null || request.getCountry().length() != 2) {
            throw new IllegalArgumentException("country must be an ISO 3166 alpha-2 code");
        }
        LocalDate dateOfBirth = parseDateOfBirth(request.getDateOfBirth());
        String nationalId = normalise(request.getNationalId());

        KycProfile profile = repository.findById(request.getUserId()).orElseGet(KycProfile::new);
        profile.setUserId(request.getUserId());
        if (profile.getReference() == null) {
            profile.setReference("KYC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        }
        profile.setFullName(request.getFullName());
        profile.setDateOfBirth(dateOfBirth);
        profile.setNationalId(nationalId);
        profile.setCountry(request.getCountry().toUpperCase(Locale.ROOT));
        profile.setStatus(decide(request.getUserId(), nationalId, dateOfBirth));
        profile.setSubmittedAt(Instant.now(clock));

        KycProfile saved = repository.save(profile);
        log.info("kyc case {} for user {} recorded with status {}", saved.getReference(), saved.getUserId(), saved.getStatus());
        return new KycStatusDto(saved.getReference(), saved.getStatus());
    }

    public KycStatusDto status(Long userId) {
        return repository.findById(userId)
                .map(p -> new KycStatusDto(p.getReference(), p.getStatus()))
                .orElse(null);
    }

    private String decide(Long userId, String nationalId, LocalDate dateOfBirth) {
        if (Period.between(dateOfBirth, LocalDate.now(clock)).getYears() < MINIMUM_AGE_YEARS) {
            return "REJECTED";
        }
        if (repository.existsByNationalIdAndUserIdNot(nationalId, userId)) {
            return "PENDING_REVIEW";
        }
        return "VERIFIED";
    }

    private static LocalDate parseDateOfBirth(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("dateOfBirth must be an ISO date (yyyy-MM-dd)");
        }
    }

    private static String normalise(String nationalId) {
        if (nationalId == null || nationalId.isBlank()) {
            throw new IllegalArgumentException("nationalId is required");
        }
        return nationalId.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }
}
