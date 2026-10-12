package com.example.lending.loan.servicing.settlement.callbacks;

import com.example.lending.loan.servicing.common.ServicingResult;
import com.example.lending.loan.servicing.settlement.instructions.SettlementInstructionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Status callbacks from settlement partners. */
@RestController
@RequestMapping("/servicing/settlement/callbacks")
public class SettlementCallbackController {

    /** Status update sent by a partner for a settlement instruction. */
    public record SettlementStatusCallback(@NotBlank String instructionId,
                                           @Pattern(regexp = "^(ACCEPTED|SETTLED|REJECTED)$") String status,
                                           String reasonCode) {
    }

    private final SettlementInstructionService instructionService;

    public SettlementCallbackController(SettlementInstructionService instructionService) {
        this.instructionService = instructionService;
    }

    @PostMapping("/{partnerId}/status")
    public ServicingResult<Void> status(@PathVariable String partnerId,
                                        @Valid @RequestBody SettlementStatusCallback callback) {
        instructionService.applyStatus(partnerId, callback.instructionId(), callback.status(), callback.reasonCode());
        return ServicingResult.ok();
    }
}
