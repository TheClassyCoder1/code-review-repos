package com.example.lending.ops.part7.promise;

import com.example.lending.ops.common.Result;
import com.example.lending.ops.part7.collections.AgentContext;
import com.example.lending.ops.part7.collections.CaseConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/console/collections/cases/{caseId}/promises")
@RequiredArgsConstructor
public class PaymentPromiseController {

    private final PaymentPromiseService promiseService;

    @GetMapping
    public Result<List<PaymentPromise>> history(@PathVariable Long caseId) {
        return Result.OK(promiseService.history(caseId));
    }

    @PostMapping
    public Result<PaymentPromise> capture(@PathVariable Long caseId,
                                          @RequestParam BigDecimal amount,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.OK(promiseService.capture(caseId, AgentContext.currentAgentId(), amount, date));
    }

    @PostMapping("/{promiseId}/cancel")
    public Result<PaymentPromise> cancel(@PathVariable Long caseId, @PathVariable Long promiseId) {
        return Result.OK(promiseService.cancel(promiseId, AgentContext.currentAgentId()));
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
}
