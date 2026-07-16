package com.example.lending.risk.repository.primary;

import com.example.lending.risk.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

/** Bound to the PRIMARY datasource (db-primary, schema lending). */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
