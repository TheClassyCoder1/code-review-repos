package com.example.lending.loan.reporting;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, String> {

    ReportSchedule findByPath(String path);
}
