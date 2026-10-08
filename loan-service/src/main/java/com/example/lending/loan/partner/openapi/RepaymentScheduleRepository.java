package com.example.lending.loan.partner.openapi;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, Long> {

    RepaymentSchedule findByPortfolioIdAndScheduleId(String portfolioId, String scheduleId);

    List<RepaymentSchedule> findByTenantIdAndPortfolioId(Long tenantId, String portfolioId);
}
