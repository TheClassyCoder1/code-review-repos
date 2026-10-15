package com.acme.claims.security;

import jakarta.servlet.http.HttpServletRequest;

public record RequestUser(long id, String role) {

    public static final String ATTRIBUTE = "claims.requestUser";

    public static RequestUser from(HttpServletRequest request) {
        return (RequestUser) request.getAttribute(ATTRIBUTE);
    }

    public boolean isAdjuster() {
        return "ADJUSTER".equals(role) || "SUPERVISOR".equals(role);
    }
}
