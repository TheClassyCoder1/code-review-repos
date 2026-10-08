package com.example.lending.loan.bureau;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BureauReportRecordRepository extends JpaRepository<BureauReportRecord, Long> {

    Optional<BureauReportRecord> findByBureauReference(String bureauReference);
}
