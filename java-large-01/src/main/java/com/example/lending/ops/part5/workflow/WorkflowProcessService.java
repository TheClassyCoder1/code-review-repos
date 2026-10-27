package com.example.lending.ops.part5.workflow;

import java.util.List;

/** Workflow definition lookups used by the batch scheduler screens. */
public interface WorkflowProcessService {

    List<WorkflowTaskRelation> findRelationByCode(long workflowDefinitionCode, int workflowDefinitionVersion);
}
