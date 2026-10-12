package com.example.lending.loan.servicing.settlement.instructions;

import com.example.lending.loan.servicing.settlement.crypto.AccountNumberCipher;
import com.example.lending.loan.servicing.settlement.partners.SettlementPartner;
import com.example.lending.loan.servicing.settlement.partners.SettlementPartnerRepository;
import com.example.lending.loan.servicing.settlement.tls.SettlementGatewayClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Sends newly created settlement instructions to the gateway. */
@Component
public class SettlementDispatchJob {

    private static final Logger log = LoggerFactory.getLogger(SettlementDispatchJob.class);

    private final SettlementPartnerRepository partners;
    private final SettlementInstructionService instructionService;
    private final SettlementInstructionRepository instructions;
    private final SettlementGatewayClient gatewayClient;

    public SettlementDispatchJob(SettlementPartnerRepository partners, SettlementInstructionService instructionService,
                                 SettlementInstructionRepository instructions, SettlementGatewayClient gatewayClient) {
        this.partners = partners;
        this.instructionService = instructionService;
        this.instructions = instructions;
        this.gatewayClient = gatewayClient;
    }

    @Scheduled(fixedDelayString = "${servicing.settlement.dispatch-interval-ms:60000}")
    public void dispatch() {
        for (SettlementPartner partner : partners.findAll()) {
            for (SettlementInstruction instruction : instructionService.pending(partner.getId())) {
                try {
                    int status = gatewayClient.submit(partner.getId(), Map.of(
                            "id", instruction.getId(),
                            "loanId", instruction.getLoanId(),
                            "amount", instruction.getAmount(),
                            "currency", instruction.getCurrency(),
                            "payoutAccount", AccountNumberCipher.decryptHex(instruction.getPayoutAccount())));
                    if (status / 100 == 2) {
                        instruction.setStatus(SettlementInstruction.Status.SENT.name());
                        instructions.save(instruction);
                    } else {
                        log.warn("Gateway rejected instruction {} with status {}", instruction.getId(), status);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    log.warn("Dispatch of instruction {} failed: {}", instruction.getId(), e.toString());
                }
            }
        }
    }
}
