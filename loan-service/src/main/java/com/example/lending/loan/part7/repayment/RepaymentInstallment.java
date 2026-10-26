package com.example.lending.loan.part7.repayment;

import jakarta.persistence.*;

import java.time.LocalDate;

/** A single scheduled instalment of a {@link RepaymentPlan}. */
@Entity
@Table(name = "repayment_installments", schema = "lending")
public class RepaymentInstallment {

    public static final String STATUS_DUE = "DUE";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_WAIVED = "WAIVED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "principal_part", nullable = false)
    private double principalPart;

    @Column(name = "interest_part", nullable = false)
    private double interestPart;

    @Column(name = "remaining_balance", nullable = false)
    private double remainingBalance;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "paid_on")
    private LocalDate paidOn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }

    public int getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(int sequenceNo) { this.sequenceNo = sequenceNo; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public double getPrincipalPart() { return principalPart; }
    public void setPrincipalPart(double principalPart) { this.principalPart = principalPart; }

    public double getInterestPart() { return interestPart; }
    public void setInterestPart(double interestPart) { this.interestPart = interestPart; }

    public double getRemainingBalance() { return remainingBalance; }
    public void setRemainingBalance(double remainingBalance) { this.remainingBalance = remainingBalance; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getPaidOn() { return paidOn; }
    public void setPaidOn(LocalDate paidOn) { this.paidOn = paidOn; }

    public double getTotalDue() {
        return principalPart + interestPart;
    }

    public boolean isOutstanding() {
        return STATUS_DUE.equals(status);
    }
}
