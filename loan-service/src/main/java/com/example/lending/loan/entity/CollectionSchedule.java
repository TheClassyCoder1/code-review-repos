package com.example.lending.loan.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "collection_schedules", schema = "lending")
public class CollectionSchedule {

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "name", unique = true)
    private String name;

    @Column(name = "status")
    private String status;

    @Column(name = "config")
    private String config;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }
}
