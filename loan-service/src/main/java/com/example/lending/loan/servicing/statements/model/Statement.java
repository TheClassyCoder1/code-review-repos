package com.example.lending.loan.servicing.statements.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/** Monthly loan statement. */
@Entity
@Table(name = "servicing_statements", schema = "lending")
public class Statement {

    public enum Status { DRAFT, RENDERED, PUBLISHED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "loan_id", nullable = false)
    private Long loanId;
    @Column(name = "borrower_id", nullable = false)
    private Long borrowerId;
    @Column(name = "period", nullable = false)
    private String period;
    @Column(name = "opening_balance", nullable = false)
    private BigDecimal openingBalance;
    @Column(name = "closing_balance", nullable = false)
    private BigDecimal closingBalance;
    @Column(name = "total_paid", nullable = false)
    private BigDecimal totalPaid;
    @Column(name = "status", nullable = false)
    private String status = Status.DRAFT.name();
    @Column(name = "version", nullable = false)
    private int version = 1;
    @Column(name = "archive_key")
    private String archiveKey;
    @Column(name = "published_at")
    private Instant publishedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }
    public Long getBorrowerId() { return borrowerId; }
    public void setBorrowerId(Long borrowerId) { this.borrowerId = borrowerId; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public BigDecimal getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(BigDecimal openingBalance) { this.openingBalance = openingBalance; }
    public BigDecimal getClosingBalance() { return closingBalance; }
    public void setClosingBalance(BigDecimal closingBalance) { this.closingBalance = closingBalance; }
    public BigDecimal getTotalPaid() { return totalPaid; }
    public void setTotalPaid(BigDecimal totalPaid) { this.totalPaid = totalPaid; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getArchiveKey() { return archiveKey; }
    public void setArchiveKey(String archiveKey) { this.archiveKey = archiveKey; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
}
