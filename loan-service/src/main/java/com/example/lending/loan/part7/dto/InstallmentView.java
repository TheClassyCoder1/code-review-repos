package com.example.lending.loan.part7.dto;

import com.example.lending.loan.part7.entity.RepaymentInstallment;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Read model for a single installment; the entity itself is never serialized. */
public class InstallmentView {

    private int sequenceNo;
    private LocalDate dueDate;
    private BigDecimal principal;
    private BigDecimal interest;
    private BigDecimal total;
    private BigDecimal remainingBalance;
    private String status;

    public InstallmentView() {
    }

    public static InstallmentView from(RepaymentInstallment installment) {
        InstallmentView view = new InstallmentView();
        view.sequenceNo = installment.getSequenceNo();
        view.dueDate = installment.getDueDate();
        view.principal = installment.getPrincipal();
        view.interest = installment.getInterest();
        view.total = installment.getTotal();
        view.remainingBalance = installment.getRemainingBalance();
        view.status = installment.getStatus();
        return view;
    }

    public int getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(int sequenceNo) { this.sequenceNo = sequenceNo; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public BigDecimal getPrincipal() { return principal; }
    public void setPrincipal(BigDecimal principal) { this.principal = principal; }

    public BigDecimal getInterest() { return interest; }
    public void setInterest(BigDecimal interest) { this.interest = interest; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public BigDecimal getRemainingBalance() { return remainingBalance; }
    public void setRemainingBalance(BigDecimal remainingBalance) { this.remainingBalance = remainingBalance; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
