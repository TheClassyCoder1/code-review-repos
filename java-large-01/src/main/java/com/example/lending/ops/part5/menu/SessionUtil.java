package com.example.lending.ops.part5.menu;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Reads facts about the current console session. */
public final class SessionUtil {

    static final String ROOT_AUTHORITY = "ROLE_ROOT";

    private SessionUtil() {
    }

    /** True only for the root super-administrator. */
    public static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated()
                && auth.getAuthorities().stream().anyMatch(a -> ROOT_AUTHORITY.equals(a.getAuthority()));
    }
}
