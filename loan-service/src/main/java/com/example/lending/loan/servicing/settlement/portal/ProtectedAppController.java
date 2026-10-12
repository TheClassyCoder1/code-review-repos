package com.example.lending.loan.servicing.settlement.portal;

import com.example.lending.loan.servicing.security.OperatorPrincipal;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/** Re-authentication step before an operator opens a protected partner application (bank portals). */
@Controller
public class ProtectedAppController {

    static final String CURRENT_SINGLESIGNON_URI = "servicing.current_singlesignon_uri";

    private final PasswordEncoder passwordEncoder;

    public ProtectedAppController(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/servicing/settlement/apps/protected")
    public ModelAndView authorizeProtected(
            @RequestParam String password,
            @RequestParam("redirect_uri") String redirectUri,
            @AuthenticationPrincipal OperatorPrincipal currentUser,
            HttpSession session) {
        if (currentUser.getAppLoginPassword() != null && passwordEncoder.matches(password, currentUser.getAppLoginPassword())) {
            session.setAttribute(CURRENT_SINGLESIGNON_URI, redirectUri);
            return new ModelAndView("redirect:" + redirectUri);
        }

        ModelAndView modelAndView = new ModelAndView("settlement/protected/forward");
        modelAndView.addObject("redirect_uri", redirectUri);
        return modelAndView;
    }
}
