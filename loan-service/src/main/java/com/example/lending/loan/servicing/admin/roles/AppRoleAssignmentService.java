package com.example.lending.loan.servicing.admin.roles;

import com.example.lending.loan.servicing.common.CurrentOperator;
import com.example.lending.loan.servicing.common.ServicingException;
import com.example.lending.loan.servicing.security.OperatorAccountRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/** Membership of operators in application roles ({@code <appId>:<roleType>}). */
@Service
public class AppRoleAssignmentService {

    private static final Set<String> ROLE_TYPES = Set.of("Master", "ModifyNamespace", "ReleaseNamespace");

    private final JdbcTemplate jdbcTemplate;
    private final OperatorAccountRepository operators;
    private final CurrentOperator currentOperator;
    private final RoleMembershipQueries membershipQueries;

    public AppRoleAssignmentService(JdbcTemplate jdbcTemplate, OperatorAccountRepository operators,
                                    CurrentOperator currentOperator, RoleMembershipQueries membershipQueries) {
        this.jdbcTemplate = jdbcTemplate;
        this.operators = operators;
        this.currentOperator = currentOperator;
        this.membershipQueries = membershipQueries;
    }

    @Transactional
    public void assignAppRoleToUser(String appId, String roleType, String userId, long operatorId) {
        if (!ROLE_TYPES.contains(roleType)) {
            throw ServicingException.badRequest("Unknown role type");
        }
        long targetId = operators.findByUsername(userId)
                .filter(account -> account.getTenantId().equals(currentOperator.tenantId()))
                .orElseThrow(() -> ServicingException.badRequest("Unknown user"))
                .getId();
        String roleKey = appId + ":" + roleType;
        if (membershipQueries.isMember(roleKey, targetId)) {
            return;
        }
        jdbcTemplate.update("INSERT INTO " + RoleMembershipQueries.TABLE
                + " (role_key, operator_id, granted_by) VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
                roleKey, targetId, operatorId);
    }
}
