package com.example.lending.loan.servicing.settlement.sso;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/** Sends sessions that did not sign in through a partner identity provider straight to the signed-out page. */
@Component
public class PartnerSsoSessionInterceptor implements HandlerInterceptor {

    /** Session attributes and paths of the partner identity provider sign-in. */
    public static final class PartnerSsoConstants {

        public static final String URL_CONTEXT = "/servicing/sso";
        public static final String PARTNER_IDP_ENTITY_ID = "partner_idp_entity_id";
        public static final String SIGNED_OUT = URL_CONTEXT + "/signed-out";

        private PartnerSsoConstants() {
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(PartnerSsoConstants.PARTNER_IDP_ENTITY_ID) == null) {
            response.sendRedirect(request.getContextPath() + PartnerSsoConstants.SIGNED_OUT);
            return false;
        }
        return true;
    }
}
