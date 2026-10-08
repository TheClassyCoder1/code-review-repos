package com.example.lending.loan.export;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface ReportExportRepository extends JpaRepository<ReportExportJob, Long> {

    List<ReportExportJob> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<ReportExportJob> findByIdAndOwnerId(Long id, Long ownerId);
}
