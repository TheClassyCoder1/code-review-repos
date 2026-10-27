package com.example.lending.ops.part5.workflow;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowProcessServiceImpl implements WorkflowProcessService {

    private final WorkflowTaskRelationLogMapper workflowTaskRelationLogMapper;

    @Override
    public List<WorkflowTaskRelation> findRelationByCode(long workflowDefinitionCode, int workflowDefinitionVersion) {
        List<WorkflowTaskRelationLog> workflowTaskRelationLogList = workflowTaskRelationLogMapper
                .queryByWorkflowCodeAndVersion(workflowDefinitionCode, workflowDefinitionVersion);
        return workflowTaskRelationLogList.stream().map(r -> (WorkflowTaskRelation) r).collect(Collectors.toList());
    }
}
