package com.example.lending.loan.partner.statement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loan_statements", schema = "lending")
public class LoanStatement {

    @Id
    private Long id;

    @Column(name = "borrower_id")
    private Long borrowerId;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "closing_balance")
    private BigDecimal closingBalance;

    public Long getId() { return id; }
    public Long getBorrowerId() { return borrowerId; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public BigDecimal getClosingBalance() { return closingBalance; }
}
