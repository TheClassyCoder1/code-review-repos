package com.example.lending.platform.common.dto;

/** Shared account view. Returned by account-service, consumed by billing-service (Feign). */
public class AccountDto {
    private Long id;
    private String name;
    private String status;
    private double balance;
    private String taxId;
    private String primaryCardNumber;
    private String dateOfBirth;

    public AccountDto() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }

    public String getPrimaryCardNumber() { return primaryCardNumber; }
    public void setPrimaryCardNumber(String primaryCardNumber) { this.primaryCardNumber = primaryCardNumber; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    @Override
    public String toString() {
        return "AccountDto{id=" + id + ", name=" + name + ", taxId=" + taxId
                + ", card=" + primaryCardNumber + ", dob=" + dateOfBirth
                + ", balance=" + balance + "}";
    }
}
