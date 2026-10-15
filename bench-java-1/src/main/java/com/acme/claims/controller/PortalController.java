package com.acme.claims.controller;

import com.acme.claims.config.ClaimsProperties;
import com.acme.claims.model.ReturnTarget;
import com.acme.claims.security.ClaimAccess;
import com.acme.claims.security.RequestUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/portal")
public class PortalController {

    private final ClaimAccess access;
    private final ClaimsProperties props;

    public PortalController(ClaimAccess access, ClaimsProperties props) {
        this.access = access;
        this.props = props;
    }

    @GetMapping("/upload-complete")
    public String afterUpload(@RequestParam long claimId,
                              @RequestParam(defaultValue = "/claims") String returnTo,
                              HttpServletRequest request) {
        access.requireOwned(claimId, RequestUser.from(request));
        return "redirect:" + returnTo;
    }

    @GetMapping("/claims/{claimId}")
    public String claimPage(@PathVariable long claimId, HttpServletRequest request) {
        access.requireOwned(claimId, RequestUser.from(request));
        return "redirect:" + props.portalBaseUrl() + "/claims/" + claimId;
    }

    @GetMapping("/back")
    public String back(@RequestParam(defaultValue = "DASHBOARD") ReturnTarget target) {
        return "redirect:" + props.portalBaseUrl() + target.path();
    }
}
