package com.example.lending.ops.part7.restructure;

import com.example.lending.ops.common.Result;
import com.example.lending.ops.part7.collections.AgentContext;
import com.example.lending.ops.part7.collections.CaseConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/console/collections")
@RequiredArgsConstructor
public class RestructureController {

    private final RestructureService restructureService;

    @PostMapping("/cases/{caseId}/restructure/preview")
    public Result<RestructurePlan> preview(@PathVariable Long caseId, @RequestBody RestructureRequest request) {
        return Result.OK(restructureService.preview(caseId, request));
    }

    @PostMapping("/cases/{caseId}/restructure")
    public Result<RestructurePlan> propose(@PathVariable Long caseId, @RequestBody RestructureRequest request) {
        return Result.OK(restructureService.propose(caseId, AgentContext.currentAgentId(), request));
    }

    @GetMapping("/cases/{caseId}/restructure")
    public Result<List<RestructurePlan>> history(@PathVariable Long caseId) {
        return Result.OK(restructureService.history(caseId));
    }

    @PostMapping("/restructure/{planId}/approve")
    public Result<RestructurePlan> approve(@PathVariable Long planId) {
        requireSupervisor();
        return Result.OK(restructureService.approve(planId, AgentContext.currentAgentId()));
    }

    @PostMapping("/restructure/{planId}/reject")
    public Result<RestructurePlan> reject(@PathVariable Long planId) {
        requireSupervisor();
        return Result.OK(restructureService.reject(planId, AgentContext.currentAgentId()));
    }

    @PostMapping("/restructure/{planId}/activate")
    public Result<RestructurePlan> activate(@PathVariable Long planId) {
        return Result.OK(restructureService.activate(planId, AgentContext.currentAgentId()));
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
