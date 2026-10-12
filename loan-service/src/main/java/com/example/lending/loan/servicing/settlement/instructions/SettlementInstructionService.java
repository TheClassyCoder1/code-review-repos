package com.example.lending.loan.servicing.settlement.instructions;

import com.example.lending.loan.servicing.common.ServicingException;
import com.example.lending.loan.servicing.settlement.crypto.AccountNumberCipher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Settlement instructions and their partner status. */
@Service
public class SettlementInstructionService {

    private static final Set<String> FINAL_STATES = Set.of(
            SettlementInstruction.Status.SETTLED.name(), SettlementInstruction.Status.REJECTED.name());

    private final SettlementInstructionRepository repository;

    public SettlementInstructionService(SettlementInstructionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public String create(String partnerId, Long loanId, BigDecimal amount, String currency, String payoutAccount) {
        SettlementInstruction instruction = new SettlementInstruction();
        instruction.setId(UUID.randomUUID().toString());
        instruction.setPartnerId(partnerId);
        instruction.setLoanId(loanId);
        instruction.setAmount(amount);
        instruction.setCurrency(currency);
        instruction.setPayoutAccount(AccountNumberCipher.encryptHex(payoutAccount));
        return repository.save(instruction).getId();
    }

    @Transactional(readOnly = true)
    public List<SettlementInstruction> pending(String partnerId) {
        return repository.findByPartnerIdAndStatus(partnerId, SettlementInstruction.Status.CREATED.name());
    }

    @Transactional
    public void applyStatus(String partnerId, String instructionId, String status, String reasonCode) {
        SettlementInstruction instruction = repository.findByIdAndPartnerId(instructionId, partnerId)
                .orElseThrow(() -> ServicingException.notFound("Instruction " + instructionId + " not found"));
        if (FINAL_STATES.contains(instruction.getStatus())) {
            throw ServicingException.conflict("Instruction " + instructionId + " is already " + instruction.getStatus());
        }
        instruction.setStatus(status);
        instruction.setReasonCode(reasonCode);
    }
}
