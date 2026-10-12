package com.example.lending.loan.servicing.collections.workflow;

/** Running collections case. */
public class CaseExecution {

    private final String caseId;
    private final String debtorIdentifier;
    private final String workflowKey;

    public CaseExecution(String caseId, String debtorIdentifier, String workflowKey) {
        this.caseId = caseId;
        this.debtorIdentifier = debtorIdentifier;
        this.workflowKey = workflowKey;
    }

    public String getCaseId() { return caseId; }
    public String getDebtorIdentifier() { return debtorIdentifier; }

    public String getWorkflowKey() { return workflowKey; }
}
