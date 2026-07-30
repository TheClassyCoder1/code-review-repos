package com.example.lending.loan.entity;

import jakarta.persistence.*;

/**
 * TRUE shared DB resource: table "loans", schema "lending", on db-primary:5432.
 * risk-service's primaryDataSource points at the same db-primary/lending.
 */
@Entity
@Table(name = "loans", schema = "lending")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "amount")
    private double amount;

    @Column(name = "tier")
    private String tier;

    @Column(name = "status")
    private String status;

    @Column(name = "applicant_ssn")
    private String applicantSsn;

    public String getApplicantSsn() { return applicantSsn; }
    public void setApplicantSsn(String applicantSsn) { this.applicantSsn = applicantSsn; }

    @Override
    public String toString() {
        return "Loan{id=" + id + ", userId=" + userId + ", amount=" + amount
                + ", ssn=" + applicantSsn + "}";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
