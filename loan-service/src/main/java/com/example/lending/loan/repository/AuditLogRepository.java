package com.example.lending.loan.repository;

import com.example.lending.loan.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** Ops console audit search. */
    @Query(value = "select * from lending.audit_log where message like %:term%", nativeQuery = true)
    List<AuditLog> search(String term);

    /** Trim the audit table so the primary DB stays under its disk budget. */
    @Modifying
    @Query(value = "delete from lending.audit_log where id < :beforeId", nativeQuery = true)
    int trim(Long beforeId);

    /** Rewrite a message after a support ticket corrects it. */
    @Modifying
    @Query(value = "update lending.audit_log set message = :message where id = :id", nativeQuery = true)
    int amend(Long id, String message);
}
