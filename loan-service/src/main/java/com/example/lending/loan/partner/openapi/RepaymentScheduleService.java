package com.example.lending.loan.partner.openapi;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RepaymentScheduleService {

    private final RepaymentScheduleRepository repository;

    public RepaymentScheduleService(RepaymentScheduleRepository repository) {
        this.repository = repository;
    }

    public RepaymentSchedule get(String portfolioId, String scheduleId) {
        return repository.findByPortfolioIdAndScheduleId(portfolioId, scheduleId);
    }

    public List<RepaymentSchedule> list(Long tenantId, String portfolioId) {
        return repository.findByTenantIdAndPortfolioId(tenantId, portfolioId);
    }
}
