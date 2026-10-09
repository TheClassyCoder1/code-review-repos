package com.example.lending.loan.servicing.settlement.instructions;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Payout or collection order sent to a settlement partner. */
@Entity
@Table(name = "settlement_instructions", schema = "lending")
public class SettlementInstruction {

    public enum Status { CREATED, SENT, ACCEPTED, SETTLED, REJECTED }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;
    @Column(name = "partner_id", nullable = false)
    private String partnerId;
    @Column(name = "loan_id", nullable = false)
    private Long loanId;
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;
    @Column(name = "currency", nullable = false)
    private String currency;
    @Column(name = "payout_account", nullable = false)
    private String payoutAccount;
    @Column(name = "status", nullable = false)
    private String status = Status.CREATED.name();
    @Column(name = "reason_code")
    private String reasonCode;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getPayoutAccount() { return payoutAccount; }
    public void setPayoutAccount(String payoutAccount) { this.payoutAccount = payoutAccount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
}
