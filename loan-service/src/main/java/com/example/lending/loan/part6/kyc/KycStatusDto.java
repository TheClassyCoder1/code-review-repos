package com.example.lending.loan.part6.kyc;

/** Response of the KYC endpoints. Carries the case reference only, never identity data. */
public class KycStatusDto {
    private String reference;
    private String status;

    public KycStatusDto() {
    }

    public KycStatusDto(String reference, String status) {
        this.reference = reference;
        this.status = status;
    }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
