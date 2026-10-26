package com.example.lending.loan.part7.topup;

import jakarta.persistence.*;

import java.time.Instant;

/** Audit row for an increase of an existing loan's principal. */
@Entity
@Table(name = "loan_top_ups", schema = "lending")
public class LoanTopUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "previous_principal", nullable = false)
    private double previousPrincipal;

    @Column(name = "additional_amount", nullable = false)
    private double additionalAmount;

    @Column(name = "new_plan_id")
    private Long newPlanId;

    @Column(name = "reason_code")
    private String reasonCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public double getPreviousPrincipal() { return previousPrincipal; }
    public void setPreviousPrincipal(double previousPrincipal) { this.previousPrincipal = previousPrincipal; }

    public double getAdditionalAmount() { return additionalAmount; }
    public void setAdditionalAmount(double additionalAmount) { this.additionalAmount = additionalAmount; }

    public Long getNewPlanId() { return newPlanId; }
    public void setNewPlanId(Long newPlanId) { this.newPlanId = newPlanId; }

    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
