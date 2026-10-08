package com.example.lending.loan.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "partner_products", schema = "lending")
public class PartnerProduct {

    @Id
    @Column(name = "identifier", nullable = false, updatable = false)
    private String identifier;

    @Column(name = "name")
    private String name;

    @Column(name = "rate")
    private BigDecimal rate;

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
}
