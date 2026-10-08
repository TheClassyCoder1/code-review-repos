package com.example.lending.loan.partner.openapi;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "repayment_schedules", schema = "lending")
public class RepaymentSchedule {

    @Id
    private Long id;

    @Column(name = "portfolio_id")
    private String portfolioId;

    @Column(name = "schedule_id")
    private String scheduleId;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    @Column(name = "outstanding")
    private BigDecimal outstanding;

    public Long getId() { return id; }
    public String getPortfolioId() { return portfolioId; }
    public String getScheduleId() { return scheduleId; }
    public Long getTenantId() { return tenantId; }
    public LocalDate getNextDueDate() { return nextDueDate; }
    public BigDecimal getOutstanding() { return outstanding; }
}
