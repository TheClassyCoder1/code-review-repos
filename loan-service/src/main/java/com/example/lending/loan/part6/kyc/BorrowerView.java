package com.example.lending.loan.part6.kyc;

/** Response for the borrower endpoints. Never carries the national id. */
public class BorrowerView {
    private final Long userId;
    private final String verificationStatus;
    private final String countryCode;

    public BorrowerView(Long userId, String verificationStatus, String countryCode) {
        this.userId = userId;
        this.verificationStatus = verificationStatus;
        this.countryCode = countryCode;
    }

    public static BorrowerView of(BorrowerIdentity identity) {
        return new BorrowerView(identity.getUserId(), identity.getVerificationStatus(), identity.getCountryCode());
    }

    public Long getUserId() { return userId; }

    public String getVerificationStatus() { return verificationStatus; }

    public String getCountryCode() { return countryCode; }
}
