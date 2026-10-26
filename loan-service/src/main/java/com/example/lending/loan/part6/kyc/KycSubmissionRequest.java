package com.example.lending.loan.part6.kyc;

/** Body of POST /api/v1/kyc, sent by portal-bff KycClient. */
public class KycSubmissionRequest {
    private Long userId;
    private String fullName;
    private String dateOfBirth;
    private String nationalId;
    private String country;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}
