package com.example.lending.ops.part5.workflow;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/** Versioned snapshot of a task relation, kept for every published workflow version. */
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkflowTaskRelationLog extends WorkflowTaskRelation {
    private long operatorUserId;
    private Instant operateTime;
}
