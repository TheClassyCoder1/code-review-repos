package com.example.lending.ops.part5.permission;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/** Answers permission questions from the console role tables. */
@Service
@RequiredArgsConstructor
public class PermissionOpenApiService {

    private final JdbcTemplate jdbcTemplate;

    public Map<String, Boolean> hasCreateApplicationPermission(String userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM console_user_role ur "
                        + "JOIN console_role_permission rp ON rp.role_id = ur.role_id "
                        + "WHERE ur.user_id = ? AND rp.permission = 'application:create'",
                Integer.class, userId);
        return Map.of("hasPermission", count != null && count > 0);
    }
}
