package com.example.lending.loan.servicing.settlement.partners;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementPartnerRepository extends JpaRepository<SettlementPartner, String> {

    List<SettlementPartner> findByTenantIdOrderByName(String tenantId);
}
