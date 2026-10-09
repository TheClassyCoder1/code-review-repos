package com.example.lending.loan.servicing.admin.roles;

import com.example.lending.loan.servicing.common.ServicingException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;

/** Grants back-office resources to roles. */
@Service
public class OperatorRoleService {

    private final JdbcTemplate jdbcTemplate;

    public OperatorRoleService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Replaces the resources of a role; returns the number of granted resources. */
    @Transactional
    public int allocResource(Long roleId, List<Long> resourceIds) {
        Integer roles = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lending.servicing_roles WHERE id = ?", Integer.class, roleId);
        if (roles == null || roles == 0) {
            throw ServicingException.notFound("Role " + roleId + " not found");
        }
        jdbcTemplate.update("DELETE FROM lending.servicing_role_resources WHERE role_id = ?", roleId);
        int count = 0;
        for (Long resourceId : new LinkedHashSet<>(resourceIds)) {
            count += jdbcTemplate.update(
                    "INSERT INTO lending.servicing_role_resources (role_id, resource_id) "
                            + "SELECT ?, id FROM lending.servicing_resources WHERE id = ?", roleId, resourceId);
        }
        return count;
    }
}
