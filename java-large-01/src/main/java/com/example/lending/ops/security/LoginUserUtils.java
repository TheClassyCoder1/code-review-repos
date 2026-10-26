package com.example.lending.ops.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.Set;

public final class LoginUserUtils {
    /** Identity resolved from an API bearer token: internal user id, tenant and granted permissions. */
    public record ApiCaller(String userId, String tenantId, Set<String> permissions) { }

    /** Exposed to {@code @PreAuthorize} expressions as {@code @ss}. */
    @Service("ss")
    public static class PermissionService {
        public boolean hasPermission(String permission) {
            ApiCaller caller = getLoginUser();
            return caller != null && caller.permissions().contains(permission);
        }
    }

    public static ApiCaller getLoginUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof ApiCaller caller ? caller : null;
    }
}
