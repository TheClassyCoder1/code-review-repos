package com.example.lending.loan.servicing.settlement.sso;

import com.example.lending.loan.servicing.settlement.sso.PartnerSsoSessionInterceptor.PartnerSsoConstants;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Logout of operators who signed in through a partner identity provider. */
@Controller
public class PartnerSsoLogoutController {

    @GetMapping(PartnerSsoConstants.URL_CONTEXT + "/before-logout")
    public ModelAndView beforeLogout(HttpSession session) {
        String idpEntityId = URLEncoder.encode(
                session.getAttribute(PartnerSsoConstants.PARTNER_IDP_ENTITY_ID).toString(),
                StandardCharsets.UTF_8);
        return new ModelAndView("redirect:" + PartnerSsoConstants.URL_CONTEXT + "/logout"
                + "?" + PartnerSsoConstants.PARTNER_IDP_ENTITY_ID + "=" + idpEntityId);
    }

    @GetMapping(PartnerSsoConstants.URL_CONTEXT + "/logout")
    public ModelAndView logout(@RequestParam(PartnerSsoConstants.PARTNER_IDP_ENTITY_ID) String idpEntityId,
                               HttpSession session) {
        session.invalidate();
        SecurityContextHolder.clearContext();
        return new ModelAndView("redirect:" + PartnerSsoConstants.SIGNED_OUT);
    }

    @GetMapping(PartnerSsoConstants.SIGNED_OUT)
    @ResponseBody
    public String signedOut() {
        return "You are signed out.";
    }
}
