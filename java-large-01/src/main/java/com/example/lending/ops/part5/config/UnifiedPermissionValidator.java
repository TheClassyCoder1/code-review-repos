package com.example.lending.ops.part5.config;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Decides whether config values in a namespace must be hidden from the current
 * user: restricted namespaces need an explicit read grant.
 */
@Component
@RequiredArgsConstructor
public class UnifiedPermissionValidator {

    private final JdbcTemplate jdbcTemplate;

    public boolean shouldHideConfigToCurrentUser(String appId, String env, String clusterName,
            String namespaceName) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer visible = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM config_namespace n WHERE n.app_id = ? AND n.env = ? AND n.cluster = ? "
                        + "AND n.name = ? AND (n.restricted = false OR EXISTS (SELECT 1 FROM config_namespace_grant g "
                        + "WHERE g.namespace_id = n.id AND g.user_id = ? AND g.can_read = true))",
                Integer.class, appId, env, clusterName, namespaceName, auth == null ? null : auth.getName());
        return visible == null || visible == 0;
    }
}
