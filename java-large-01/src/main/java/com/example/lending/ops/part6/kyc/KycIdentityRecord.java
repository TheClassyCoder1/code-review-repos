package com.example.lending.ops.part6.kyc;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

/** Identity details captured by an agent during assisted KYC onboarding. */
@Data
public class KycIdentityRecord {
    private Long id;
    private Long customerId;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String nationalId;
    private String documentType;
    private String status;
    private String verifiedBy;
    private Instant createdAt;
    private Instant updatedAt;
}
