package com.example.lending.loan.bureau;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bureau_reports", schema = "lending")
public class BureauReportRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bureau_reference", nullable = false, unique = true)
    private String bureauReference;

    @Column(name = "borrower_id", nullable = false)
    private Long borrowerId;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "revolving_utilisation", nullable = false)
    private BigDecimal revolvingUtilisation;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    @Column(name = "stale", nullable = false)
    private boolean stale;

    public Long getId() { return id; }
    public String getBureauReference() { return bureauReference; }
    public Long getBorrowerId() { return borrowerId; }
    public int getScore() { return score; }
    public BigDecimal getRevolvingUtilisation() { return revolvingUtilisation; }
    public Instant getImportedAt() { return importedAt; }
    public boolean isStale() { return stale; }

    public void refresh(String bureauReference, Long borrowerId, int score,
                        BigDecimal revolvingUtilisation, Instant importedAt) {
        this.bureauReference = bureauReference;
        this.borrowerId = borrowerId;
        this.score = score;
        this.revolvingUtilisation = revolvingUtilisation;
        this.importedAt = importedAt;
        this.stale = false;
    }

    public void markStale() {
        this.stale = true;
    }
}
