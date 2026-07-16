package com.example.lending.platform.common.event;

/**
 * Shared event payload for the "account.created" topic.
 * TRAP: billing-service produces a topic ALSO named "account.created" but on a DIFFERENT broker
 * (broker-b), using its own copied payload — a FALSE match to this shared event.
 */
public class AccountCreatedEvent {
    private Long accountId;
    private String name;

    public AccountCreatedEvent() {
    }

    public AccountCreatedEvent(Long accountId, String name) {
        this.accountId = accountId;
        this.name = name;
    }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
