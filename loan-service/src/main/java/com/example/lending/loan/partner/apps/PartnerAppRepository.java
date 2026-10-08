package com.example.lending.loan.partner.apps;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerAppRepository extends JpaRepository<PartnerApp, String> {

    List<PartnerApp> findByTenantId(Long tenantId);
}
