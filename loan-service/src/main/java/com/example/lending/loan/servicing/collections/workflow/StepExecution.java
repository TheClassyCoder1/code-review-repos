package com.example.lending.loan.servicing.collections.workflow;

/** One step of a collections case (reminder letter, call campaign, field visit, legal review). */
public class StepExecution {

    public enum State { WAITING, RUNNING, SUCCEEDED, FAILED, SKIPPED }

    private final String stepKey;
    private final CaseEventBus caseEventBus;
    private State state = State.WAITING;

    public StepExecution(String stepKey, CaseEventBus caseEventBus) {
        this.stepKey = stepKey;
        this.caseEventBus = caseEventBus;
    }

    public String getStepKey() { return stepKey; }
    public State getState() { return state; }

    public void setState(State state) { this.state = state; }
    public CaseEventBus getCaseEventBus() { return caseEventBus; }
}
