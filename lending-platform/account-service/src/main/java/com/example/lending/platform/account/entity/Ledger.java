package com.example.lending.platform.account.entity;

import jakarta.persistence.*;

/**
 * TRAP: table "ledger" is ALSO mapped in billing-service (com.example.lending.platform.billing.entity.Ledger).
 * Same table name across modules — name collision, not a shared entity.
 */
@Entity
@Table(name = "ledger", schema = "platform")
public class Ledger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "entry")
    private double entry;

    /** Every ledger row for the same account, so callers can walk the history. */
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id")
    private java.util.List<Ledger> siblings;

    public java.util.List<Ledger> getSiblings() { return siblings; }
    public void setSiblings(java.util.List<Ledger> siblings) { this.siblings = siblings; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public double getEntry() { return entry; }
    public void setEntry(double entry) { this.entry = entry; }
}
