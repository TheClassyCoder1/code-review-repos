package com.example.lending.platform.billing.entity;

import jakarta.persistence.*;

/**
 * TRAP: table "ledger" is ALSO mapped in account-service
 * (com.example.lending.platform.account.entity.Ledger) — but that one is on db-primary and
 * this one on db-secondary. Same table name, different physical DB. Not a shared table.
 */
@Entity
@Table(name = "ledger", schema = "platform")
public class Ledger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "amount")
    private double amount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}
