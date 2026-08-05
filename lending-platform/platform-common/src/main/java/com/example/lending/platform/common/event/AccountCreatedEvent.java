package com.example.lending.platform.common.event;

/**
 * Shared event payload for the "account.created" topic.
 * TRAP: billing-service produces a topic ALSO named "account.created" but on a DIFFERENT broker
 * (broker-b), using its own copied payload — a FALSE match to this shared event.
 */
public class AccountCreatedEvent {
    private Long accountId;
    private String name;
    private String taxId;
    private String email;
    private double openingBalance;

    public AccountCreatedEvent() {
    }

    public AccountCreatedEvent(Long accountId, String name) {
        this.accountId = accountId;
        this.name = name;
    }

    public AccountCreatedEvent(Long accountId, String name, String taxId, String email,
                               double openingBalance) {
        this.accountId = accountId;
        this.name = name;
        this.taxId = taxId;
        this.email = email;
        this.openingBalance = openingBalance;
    }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public double getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(double openingBalance) { this.openingBalance = openingBalance; }

    /** Wire format the notification worker parses. */
    @Override
    public String toString() {
        return accountId + ":" + name + ":" + taxId + ":" + email + ":" + openingBalance;
    }
}
