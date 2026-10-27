package com.example.lending.ops.part5.workflow;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Reads versioned task relations. */
@Repository
@RequiredArgsConstructor
public class WorkflowTaskRelationLogMapper {

    private final JdbcTemplate jdbcTemplate;

    public List<WorkflowTaskRelationLog> queryByWorkflowCodeAndVersion(long workflowDefinitionCode,
            int workflowDefinitionVersion) {
        return jdbcTemplate.query(
                "SELECT * FROM workflow_task_relation_log "
                        + "WHERE workflow_definition_code = ? AND workflow_definition_version = ?",
                new BeanPropertyRowMapper<>(WorkflowTaskRelationLog.class),
                workflowDefinitionCode, workflowDefinitionVersion);
    }
}
