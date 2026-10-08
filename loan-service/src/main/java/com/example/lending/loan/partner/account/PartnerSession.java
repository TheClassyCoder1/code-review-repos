package com.example.lending.loan.partner.account;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public final class PartnerSession {

    public static final String USER_ID = "userId";
    public static final String TENANT_ID = "tenantId";

    private PartnerSession() {
    }

    public static Long userId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (Long) session.getAttribute(USER_ID);
    }

    public static Long tenantId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (Long) session.getAttribute(TENANT_ID);
    }

    public static void userLogin(HttpServletRequest request, PartnerUser user) {
        HttpSession previous = request.getSession(false);
        if (previous != null) {
            previous.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(USER_ID, user.getId());
        session.setAttribute(TENANT_ID, user.getTenantId());
    }
}
