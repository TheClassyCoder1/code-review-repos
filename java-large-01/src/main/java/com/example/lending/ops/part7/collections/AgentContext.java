package com.example.lending.ops.part7.collections;

import com.example.lending.ops.security.LoginUserUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Resolves the collections agent behind the current request. Console sessions authenticate with
 * the agent's internal numeric id as the principal name; API callers carry it in {@link LoginUserUtils.ApiCaller}.
 */
public final class AgentContext {

    public static final String SUPERVISOR_ROLE = "ROLE_COLLECTIONS_SUPERVISOR";

    private AgentContext() {
    }

    public static Long currentAgentId() {
        LoginUserUtils.ApiCaller caller = LoginUserUtils.getLoginUser();
        if (caller != null) {
            return parse(caller.userId());
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("no authenticated agent");
        }
        return parse(auth.getName());
    }

    public static boolean isSupervisor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (SUPERVISOR_ROLE.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private static Long parse(String id) {
        try {
            return Long.valueOf(id);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("principal is not a collections agent id");
        }
    }
}
