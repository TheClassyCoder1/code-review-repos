package com.example.lending.loan.batch;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskInstanceService {

    private final JdbcTemplate jdbcTemplate;

    public TaskInstanceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void forceTaskSuccess(String loginUser, long projectCode, Integer id) {
        Integer member = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lending.batch_project_members WHERE project_code = ? AND user_name = ? AND can_operate = TRUE",
                Integer.class, projectCode, loginUser);
        if (member == null || member == 0) {
            throw new AccessDeniedException("No operate permission on project " + projectCode);
        }
        int updated = jdbcTemplate.update(
                "UPDATE lending.batch_task_instances SET state = 'FORCED_SUCCESS' "
                        + "WHERE id = ? AND project_code = ? AND state IN ('FAILURE', 'KILLED', 'PAUSED')",
                id, projectCode);
        if (updated == 0) {
            throw new IllegalStateException("Task instance " + id + " cannot be forced to success");
        }
    }
}
