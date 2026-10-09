package com.example.lending.loan.servicing.settlement.instructions;

import com.example.lending.loan.servicing.common.CurrentOperator;
import com.example.lending.loan.servicing.common.ServicingException;
import com.example.lending.loan.servicing.common.ServicingResult;
import com.example.lending.loan.servicing.settlement.partners.SettlementPartnerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/** Creation of payout instructions by treasury operators. */
@RestController
@RequestMapping("/servicing/settlement/partners/{partnerId}/instructions")
public class SettlementInstructionController {

    public record CreateInstruction(@NotNull Long loanId, @NotNull @Positive BigDecimal amount,
                                    @Pattern(regexp = "^[A-Z]{3}$") String currency,
                                    @Pattern(regexp = "^[A-Z0-9]{8,34}$") String payoutAccount) {
    }

    private final SettlementInstructionService instructionService;
    private final SettlementPartnerService partnerService;
    private final CurrentOperator currentOperator;

    public SettlementInstructionController(SettlementInstructionService instructionService,
                                           SettlementPartnerService partnerService, CurrentOperator currentOperator) {
        this.instructionService = instructionService;
        this.partnerService = partnerService;
        this.currentOperator = currentOperator;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('settlement:instruct')")
    public ServicingResult<String> create(@PathVariable String partnerId, @Valid @RequestBody CreateInstruction request) {
        if (!partnerService.get(partnerId).getTenantId().equals(currentOperator.tenantId())) {
            throw ServicingException.notFound("Settlement partner " + partnerId + " not found");
        }
        return ServicingResult.ok(instructionService.create(partnerId, request.loanId(), request.amount(),
                request.currency(), request.payoutAccount()));
    }
}
