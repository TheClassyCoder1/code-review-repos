package com.example.lending.loan.servicing.collections.workflow;

/** Applies a state change to a step and notifies the workflow topology. */
public abstract class AbstractStepStateAction {

    public void succeed(final CaseExecution caseExecution, final StepExecution stepExecution) {
        changeState(caseExecution, stepExecution, StepExecution.State.SUCCEEDED);
    }

    public void fail(final CaseExecution caseExecution, final StepExecution stepExecution) {
        changeState(caseExecution, stepExecution, StepExecution.State.FAILED);
    }

    public void skip(final CaseExecution caseExecution, final StepExecution stepExecution) {
        changeState(caseExecution, stepExecution, StepExecution.State.SKIPPED);
    }

    private void changeState(CaseExecution caseExecution, StepExecution stepExecution, StepExecution.State target) {
        if (stepExecution.getState() != StepExecution.State.RUNNING
                && stepExecution.getState() != StepExecution.State.WAITING) {
            throw new IllegalStateException("Step " + stepExecution.getStepKey() + " is already "
                    + stepExecution.getState());
        }
        stepExecution.setState(target);
        onStateChanged(caseExecution, stepExecution);
        publishCaseTopologyTransitionEvent(caseExecution, stepExecution);
    }

    protected abstract void onStateChanged(CaseExecution caseExecution, StepExecution stepExecution);

    protected void publishCaseTopologyTransitionEvent(
                                                      final CaseExecution caseExecution,
                                                      final StepExecution stepExecution) {
        stepExecution
                .getCaseEventBus()
                .publish(
                        CaseStepFinishedEvent.of(
                                caseExecution,
                                stepExecution));
    }
}
