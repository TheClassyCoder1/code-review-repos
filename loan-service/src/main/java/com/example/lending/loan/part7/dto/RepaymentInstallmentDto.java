package com.example.lending.loan.part7.dto;

import java.time.LocalDate;

/** One row of an amortization schedule. Amounts are rounded to cents. */
public class RepaymentInstallmentDto {
    private int number;
    private LocalDate dueDate;
    private double payment;
    private double principal;
    private double interest;
    private double remainingBalance;

    public RepaymentInstallmentDto() {
    }

    public RepaymentInstallmentDto(int number, LocalDate dueDate, double payment,
                                   double principal, double interest, double remainingBalance) {
        this.number = number;
        this.dueDate = dueDate;
        this.payment = payment;
        this.principal = principal;
        this.interest = interest;
        this.remainingBalance = remainingBalance;
    }

    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public double getPayment() { return payment; }
    public void setPayment(double payment) { this.payment = payment; }

    public double getPrincipal() { return principal; }
    public void setPrincipal(double principal) { this.principal = principal; }

    public double getInterest() { return interest; }
    public void setInterest(double interest) { this.interest = interest; }

    public double getRemainingBalance() { return remainingBalance; }
    public void setRemainingBalance(double remainingBalance) { this.remainingBalance = remainingBalance; }
}
