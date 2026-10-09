package com.example.lending.loan.servicing.collections.contacts;

import com.example.lending.loan.servicing.security.OperatorAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Entry in an operator's team directory. A pending entry is a request waiting for the contact to accept. */
@Entity
@Table(name = "servicing_operator_contacts", schema = "lending")
public class OperatorContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "owner_id")
    private OperatorAccount owner;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "contact_id")
    private OperatorAccount contact;
    @Column(name = "pending", nullable = false)
    private boolean pending;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public OperatorAccount getOwner() { return owner; }
    public void setOwner(OperatorAccount owner) { this.owner = owner; }
    public OperatorAccount getContact() { return contact; }
    public void setContact(OperatorAccount contact) { this.contact = contact; }
    public boolean isPending() { return pending; }
    public void setPending(boolean pending) { this.pending = pending; }
}
