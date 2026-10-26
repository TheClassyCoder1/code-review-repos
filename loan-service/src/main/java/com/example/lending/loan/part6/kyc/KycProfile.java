package com.example.lending.loan.part6.kyc;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Identity data captured during onboarding. One row per user; a resubmission overwrites
 * the previous record. Table "kyc_profiles", schema "lending", on db-primary.
 */
@Entity
@Table(name = "kyc_profiles", schema = "lending")
public class KycProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "reference", nullable = false, unique = true)
    private String reference;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "national_id", nullable = false)
    private String nationalId;

    @Column(name = "country", length = 2, nullable = false)
    private String country;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
}
