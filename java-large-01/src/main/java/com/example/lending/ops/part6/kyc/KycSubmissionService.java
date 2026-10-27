package com.example.lending.ops.part6.kyc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Validates and records identity details captured by an agent on a call with the customer. */
@Slf4j
@Service
@RequiredArgsConstructor
public class KycSubmissionService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9 ()-]{7,20}$");
    private static final Set<String> DOCUMENT_TYPES = Set.of("PASSPORT", "DRIVING_LICENCE", "NATIONAL_ID_CARD");

    private final KycDocumentStore store;

    public Long submit(Long customerId, KycIdentityRecord record) {
        if (customerId == null) {
            throw new IllegalArgumentException("customerId is required");
        }
        List<String> errors = new ArrayList<>();
        String fullName = record.getFullName() == null ? "" : record.getFullName().trim();
        if (fullName.length() < 2 || fullName.length() > 120) {
            errors.add("fullName must be between 2 and 120 characters");
        }
        String email = record.getEmail() == null ? "" : record.getEmail().trim().toLowerCase();
        if (!EMAIL.matcher(email).matches()) {
            errors.add("email is not a valid address");
        }
        String phone = record.getPhone() == null ? "" : record.getPhone().trim();
        if (!PHONE.matcher(phone).matches()) {
            errors.add("phone must contain 7 to 20 digits");
        }
        LocalDate dob = record.getDateOfBirth();
        if (dob == null || Period.between(dob, LocalDate.now()).getYears() < 18) {
            errors.add("customer must be at least 18 years old");
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }
        if (record.getDocumentType() == null || !DOCUMENT_TYPES.contains(record.getDocumentType())) {
            throw new IllegalArgumentException("unsupported documentType");
        }
        if (record.getNationalId() == null || record.getNationalId().isBlank()) {
            throw new IllegalArgumentException("nationalId is required");
        }

        record.setCustomerId(customerId);
        record.setFullName(fullName);
        record.setEmail(email);
        record.setPhone(phone);
        record.setNationalId(record.getNationalId().replaceAll("\\s", "").toUpperCase());
        record.setStatus("PENDING_REVIEW");
        Long id = store.insert(record);
        log.info("kyc identity {} captured for customer {}", id, customerId);
        return id;
    }

    public void review(Long recordId, String agentId, boolean approved) {
        int updated = approved ? store.markVerified(recordId, agentId) : store.markRejected(recordId, agentId);
        if (updated == 0) {
            throw new IllegalArgumentException("kyc identity " + recordId + " not found");
        }
        log.info("kyc identity {} reviewed by agent {} approved={}", recordId, agentId, approved);
    }

    public List<KycIdentityRecord> reviewQueue(int limit) {
        return store.findPendingReview(Math.max(1, Math.min(limit, 200)));
    }
}
