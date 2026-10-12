package com.example.lending.loan.servicing.settlement.instructions;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SettlementInstructionRepository extends JpaRepository<SettlementInstruction, String> {

    Optional<SettlementInstruction> findByIdAndPartnerId(String id, String partnerId);

    List<SettlementInstruction> findByPartnerIdAndStatus(String partnerId, String status);
}
