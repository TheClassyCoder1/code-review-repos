package com.example.lending.loan.part7.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Response of GET /api/v1/loans/{id}/schedule/payoff. */
public class PayoffQuote {

    private Long loanId;
    private LocalDate payoffDate;
    private BigDecimal outstandingPrincipal;
    private BigDecimal accruedInterest;
    private BigDecimal prepaymentFee;
    private BigDecimal payoffAmount;
    private LocalDate validUntil;

    public PayoffQuote() {
    }

    public PayoffQuote(Long loanId, LocalDate payoffDate, BigDecimal outstandingPrincipal,
                       BigDecimal accruedInterest, BigDecimal prepaymentFee,
                       BigDecimal payoffAmount, LocalDate validUntil) {
        this.loanId = loanId;
        this.payoffDate = payoffDate;
        this.outstandingPrincipal = outstandingPrincipal;
        this.accruedInterest = accruedInterest;
        this.prepaymentFee = prepaymentFee;
        this.payoffAmount = payoffAmount;
        this.validUntil = validUntil;
    }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public LocalDate getPayoffDate() { return payoffDate; }
    public void setPayoffDate(LocalDate payoffDate) { this.payoffDate = payoffDate; }

    public BigDecimal getOutstandingPrincipal() { return outstandingPrincipal; }
    public void setOutstandingPrincipal(BigDecimal outstandingPrincipal) { this.outstandingPrincipal = outstandingPrincipal; }

    public BigDecimal getAccruedInterest() { return accruedInterest; }
    public void setAccruedInterest(BigDecimal accruedInterest) { this.accruedInterest = accruedInterest; }

    public BigDecimal getPrepaymentFee() { return prepaymentFee; }
    public void setPrepaymentFee(BigDecimal prepaymentFee) { this.prepaymentFee = prepaymentFee; }

    public BigDecimal getPayoffAmount() { return payoffAmount; }
    public void setPayoffAmount(BigDecimal payoffAmount) { this.payoffAmount = payoffAmount; }

    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }
}
