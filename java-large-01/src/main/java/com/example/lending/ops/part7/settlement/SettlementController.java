package com.example.lending.ops.part7.settlement;

import com.example.lending.ops.common.Result;
import com.example.lending.ops.part7.collections.AgentContext;
import com.example.lending.ops.part7.collections.CaseConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/console/collections")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @PostMapping("/cases/{caseId}/settlements")
    public Result<SettlementOffer> create(@PathVariable Long caseId, @RequestParam BigDecimal amount) {
        return Result.OK(settlementService.createOffer(caseId, AgentContext.currentAgentId(), amount));
    }

    @GetMapping("/cases/{caseId}/settlements")
    public Result<List<SettlementOffer>> history(@PathVariable Long caseId) {
        return Result.OK(settlementService.history(caseId));
    }

    @GetMapping("/settlements/awaiting-approval")
    public Result<List<SettlementOffer>> awaiting(@RequestParam(defaultValue = "50") int limit) {
        requireSupervisor();
        return Result.OK(settlementService.awaitingSupervisor(limit));
    }

    @PostMapping("/settlements/{offerId}/approve")
    public Result<SettlementOffer> approve(@PathVariable Long offerId) {
        requireSupervisor();
        return Result.OK(settlementService.approve(offerId, AgentContext.currentAgentId()));
    }

    @PostMapping("/settlements/{offerId}/reject")
    public Result<SettlementOffer> reject(@PathVariable Long offerId) {
        requireSupervisor();
        return Result.OK(settlementService.reject(offerId, AgentContext.currentAgentId()));
    }

    @PostMapping("/settlements/{offerId}/accept")
    public Result<SettlementOffer> accept(@PathVariable Long offerId) {
        return Result.OK(settlementService.accept(offerId, AgentContext.currentAgentId()));
    }

    @PostMapping("/settlements/{offerId}/paid")
    public Result<SettlementOffer> paid(@PathVariable Long offerId) {
        return Result.OK(settlementService.markPaid(offerId, AgentContext.currentAgentId()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> notFound(NoSuchElementException e) {
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(CaseConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<Void> conflict(CaseConflictException e) {
        return Result.error(e.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> badRequest(RuntimeException e) {
        return Result.error(e.getMessage());
    }

    private void requireSupervisor() {
        if (!AgentContext.isSupervisor()) {
            throw new IllegalStateException("collections supervisor role required");
        }
    }
}
