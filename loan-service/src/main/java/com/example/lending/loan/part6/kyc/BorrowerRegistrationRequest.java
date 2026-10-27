package com.example.lending.loan.part6.kyc;

/** Body of POST /api/v1/borrowers and PUT /api/v1/borrowers/{userId}/contact. */
public class BorrowerRegistrationRequest {
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String nationalId;
    private String countryCode;

    public BorrowerRegistrationRequest() {
    }

    public BorrowerRegistrationRequest(Long userId, String fullName, String email,
                                       String phone, String nationalId, String countryCode) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.nationalId = nationalId;
        this.countryCode = countryCode;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
}
