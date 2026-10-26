package com.example.lending.loan.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * TRUE shared DB resource: table "loans", schema "lending", on db-primary:5432.
 * risk-service's primaryDataSource points at the same db-primary/lending.
 */
@Entity
@Table(name = "loans", schema = "lending")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    // Exact decimal principal; serialized as a string so clients never see binary-float artefacts.
    @Column(name = "amount", precision = 19, scale = 2)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;

    @Column(name = "tier")
    private String tier;

    @Column(name = "status")
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
