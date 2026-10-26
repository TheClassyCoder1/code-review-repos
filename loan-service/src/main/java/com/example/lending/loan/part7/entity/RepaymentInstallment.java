package com.example.lending.loan.part7.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One row of a loan's amortization schedule. Lives next to "loans" in db-primary, schema "lending".
 */
@Entity
@Table(name = "repayment_installments", schema = "lending")
public class RepaymentInstallment {

    public static final String STATUS_DUE = "DUE";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_SUPERSEDED = "SUPERSEDED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "principal", precision = 14, scale = 2, nullable = false)
    private BigDecimal principal;

    @Column(name = "interest", precision = 14, scale = 2, nullable = false)
    private BigDecimal interest;

    @Column(name = "remaining_balance", precision = 14, scale = 2, nullable = false)
    private BigDecimal remainingBalance;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "paid_on")
    private LocalDate paidOn;

    public RepaymentInstallment() {
    }

    public RepaymentInstallment(Long loanId, int sequenceNo, LocalDate dueDate,
                                BigDecimal principal, BigDecimal interest, BigDecimal remainingBalance) {
        this.loanId = loanId;
        this.sequenceNo = sequenceNo;
        this.dueDate = dueDate;
        this.principal = principal;
        this.interest = interest;
        this.remainingBalance = remainingBalance;
        this.status = STATUS_DUE;
    }

    public BigDecimal getTotal() {
        return principal.add(interest);
    }

    public boolean isOpen() {
        return STATUS_DUE.equals(status);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public int getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(int sequenceNo) { this.sequenceNo = sequenceNo; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public BigDecimal getPrincipal() { return principal; }
    public void setPrincipal(BigDecimal principal) { this.principal = principal; }

    public BigDecimal getInterest() { return interest; }
    public void setInterest(BigDecimal interest) { this.interest = interest; }

    public BigDecimal getRemainingBalance() { return remainingBalance; }
    public void setRemainingBalance(BigDecimal remainingBalance) { this.remainingBalance = remainingBalance; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getPaidOn() { return paidOn; }
    public void setPaidOn(LocalDate paidOn) { this.paidOn = paidOn; }
}
