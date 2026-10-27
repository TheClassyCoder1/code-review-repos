package com.example.lending.ops.part7.collections;

import com.example.lending.ops.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/** Collections desk endpoints for the browser console. */
@RestController
@RequestMapping("/console/collections/cases")
@RequiredArgsConstructor
public class CollectionCaseController {

    private final CollectionCaseService caseService;

    @GetMapping("/{caseId}")
    public Result<CollectionCase> get(@PathVariable Long caseId) {
        return Result.OK(caseService.get(caseId));
    }

    @GetMapping("/queue")
    public Result<List<CollectionCase>> queue(@RequestParam String tenantId,
                                              @RequestParam(defaultValue = "50") int size) {
        return Result.OK(caseService.queue(tenantId, currentAgentId(), size));
    }

    @PostMapping("/assign")
    public Result<List<CollectionCase>> assign(@RequestParam String tenantId,
                                               @RequestParam(defaultValue = "20") int count) {
        return Result.OK(caseService.assignBatch(tenantId, currentAgentId(), count));
    }

    @PostMapping("/intake")
    public Result<CollectionCase> intake(@RequestBody CollectionCaseService.DelinquencyReport report) {
        return Result.OK(caseService.openOrRefresh(report));
    }

    @PostMapping("/{caseId}/contact")
    public Result<CollectionCase> contact(@PathVariable Long caseId,
                                          @RequestParam CollectionCaseService.ContactOutcome outcome) {
        return Result.OK(caseService.recordContact(caseId, currentAgentId(), outcome));
    }

    @PostMapping("/{caseId}/close")
    public Result<CollectionCase> close(@PathVariable Long caseId, @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "CURED");
        return Result.OK(caseService.close(caseId, currentAgentId(), reason));
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

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> badRequest(IllegalArgumentException e) {
        return Result.error(e.getMessage());
    }

    private Long currentAgentId() {
        return AgentContext.currentAgentId();
    }
}
