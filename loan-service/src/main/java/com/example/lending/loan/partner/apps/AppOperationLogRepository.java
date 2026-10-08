package com.example.lending.loan.partner.apps;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppOperationLogRepository extends JpaRepository<AppOperationLog, Long> {

    List<AppOperationLog> findByAppIdAndTenantId(String appId, Long tenantId);
}
