package com.example.lending.risk.reporting.entity;

import jakarta.persistence.*;

/**
 * REPORTING datasource (db-secondary:5432, schema "lending").
 * Same schema NAME "lending" as the primary datasource, but a DIFFERENT host.
 * A tool should NOT treat db-primary.lending and db-secondary.lending as the same resource.
 */
@Entity
@Table(name = "risk_scores", schema = "lending")
public class RiskScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "score")
    private double score;

    @Column(name = "decision")
    private String decision;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
}
