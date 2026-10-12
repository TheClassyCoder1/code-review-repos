package com.example.lending.loan.servicing.settlement.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

/** Hands an operator over to the partner settlement portal. */
@Controller
@RequestMapping("/servicing/settlement/portal")
public class SettlementPortalLoginController {

    private final SettlementJwtIssuer jwtIssuer;
    private final String portalLoginUrl;

    public SettlementPortalLoginController(SettlementJwtIssuer jwtIssuer,
                                           @Value("${servicing.settlement.portal-login-url}") String portalLoginUrl) {
        this.jwtIssuer = jwtIssuer;
        this.portalLoginUrl = portalLoginUrl;
    }

    @GetMapping("/login")
    @PreAuthorize("hasAuthority('settlement:portal')")
    public ModelAndView login() {
        String target = UriComponentsBuilder.fromHttpUrl(portalLoginUrl)
                .queryParam("jwt", jwtIssuer.buildLoginJwt())
                .toUriString();
        return new ModelAndView("redirect:" + target);
    }
}
