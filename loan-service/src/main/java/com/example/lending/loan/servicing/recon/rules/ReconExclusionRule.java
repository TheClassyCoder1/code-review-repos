package com.example.lending.loan.servicing.recon.rules;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Remittance text that marks a bank line as out of scope for loan matching (fees, internal transfers). */
@Entity
@Table(name = "recon_exclusion_rules", schema = "lending")
public class ReconExclusionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "keyword", nullable = false)
    private String keyword;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
}
