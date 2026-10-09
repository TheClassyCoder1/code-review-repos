package com.example.lending.loan.servicing.collections.workflow;

import com.example.lending.loan.servicing.common.ServicingResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Collectors close the manual steps of a case (calls, visits) from the case screen. */
@RestController
@RequestMapping("/servicing/collections/cases/{caseId}/steps/{stepKey}")
public class CaseStepController {

    private final DefaultStepStateAction stepStateAction;
    private final CaseEventBus caseEventBus;

    public CaseStepController(DefaultStepStateAction stepStateAction, CaseEventBus caseEventBus) {
        this.stepStateAction = stepStateAction;
        this.caseEventBus = caseEventBus;
    }

    @PostMapping("/complete")
    @PreAuthorize("hasAuthority('case:work')")
    public ServicingResult<Void> complete(@PathVariable String caseId, @PathVariable String stepKey,
                                          @RequestParam String debtorIdentifier,
                                          @RequestParam(defaultValue = "true") boolean succeeded) {
        CaseExecution caseExecution = new CaseExecution(caseId, debtorIdentifier, "standard");
        StepExecution stepExecution = new StepExecution(stepKey, caseEventBus);
        stepExecution.setState(StepExecution.State.RUNNING);
        if (succeeded) {
            stepStateAction.succeed(caseExecution, stepExecution);
        } else {
            stepStateAction.fail(caseExecution, stepExecution);
        }
        return ServicingResult.ok();
    }
}
