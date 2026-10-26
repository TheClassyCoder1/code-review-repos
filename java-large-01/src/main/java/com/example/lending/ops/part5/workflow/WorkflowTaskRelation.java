package com.example.lending.ops.part5.workflow;

import lombok.Data;

/** Edge between two tasks in a collections or underwriting batch workflow. */
@Data
public class WorkflowTaskRelation {
    private long workflowDefinitionCode;
    private int workflowDefinitionVersion;
    private long preTaskCode;
    private int preTaskVersion;
    private long postTaskCode;
    private int postTaskVersion;
    private String conditionType;
}
