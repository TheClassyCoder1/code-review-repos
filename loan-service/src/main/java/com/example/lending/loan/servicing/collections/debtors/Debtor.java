package com.example.lending.loan.servicing.collections.debtors;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Person or business with an amount owed on a loan or fee. The identifier is the servicing account number. */
@Entity
@Table(name = "servicing_debtors", schema = "lending")
public class Debtor {

    public enum Type { PERSON, BUSINESS }

    public enum State { PENDING, ACTIVE, LOCKED, CLOSED }

    @Id
    @Column(name = "identifier", nullable = false, updatable = false)
    private String identifier;
    @Column(name = "type", nullable = false)
    private String type;
    @Column(name = "current_state", nullable = false)
    private String currentState;
    @Column(name = "member")
    private boolean member;
    @Column(name = "display_name")
    private String displayName;

    @Embedded
    private AddressEntity address;

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getCurrentState() { return currentState; }
    public void setCurrentState(String currentState) { this.currentState = currentState; }
    public boolean isMember() { return member; }
    public void setMember(boolean member) { this.member = member; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public AddressEntity getAddress() { return address; }
    public void setAddress(AddressEntity address) { this.address = address; }
}
