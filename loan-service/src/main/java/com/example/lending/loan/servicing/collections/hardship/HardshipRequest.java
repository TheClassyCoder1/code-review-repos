package com.example.lending.loan.servicing.collections.hardship;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/** Borrower request for a payment holiday or a reduced repayment plan. */
@Entity
@Table(name = "servicing_hardship_requests", schema = "lending")
public class HardshipRequest {

    public enum Status { SUBMITTED, IN_REVIEW, APPROVED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "borrower_id", nullable = false)
    private Long borrowerId;
    @Column(name = "loan_id", nullable = false)
    private Long loanId;
    @Column(name = "reason", nullable = false, length = 2000)
    private String reason;
    @Column(name = "monthly_income")
    private BigDecimal monthlyIncome;
    @Column(name = "requested_months")
    private Integer requestedMonths;
    @Column(name = "status", nullable = false)
    private String status = Status.SUBMITTED.name();
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBorrowerId() { return borrowerId; }
    public void setBorrowerId(Long borrowerId) { this.borrowerId = borrowerId; }
    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(BigDecimal monthlyIncome) { this.monthlyIncome = monthlyIncome; }
    public Integer getRequestedMonths() { return requestedMonths; }
    public void setRequestedMonths(Integer requestedMonths) { this.requestedMonths = requestedMonths; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
