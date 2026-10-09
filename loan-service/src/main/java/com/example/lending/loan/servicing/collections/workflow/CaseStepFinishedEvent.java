package com.example.lending.loan.servicing.collections.workflow;

/** A step reached a final state, so the workflow can evaluate the next transitions. */
public record CaseStepFinishedEvent(String caseId, String debtorIdentifier, String stepKey,
                                    StepExecution.State state) {

    public static CaseStepFinishedEvent of(CaseExecution caseExecution, StepExecution stepExecution) {
        return new CaseStepFinishedEvent(caseExecution.getCaseId(), caseExecution.getDebtorIdentifier(),
                stepExecution.getStepKey(), stepExecution.getState());
    }
}
