package com.example.lending.loan.servicing.security.portal;

import com.example.lending.loan.servicing.common.ServicingException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Access to the borrower bound to the current portal request. */
@Component
public class CurrentBorrower {

    public PortalPrincipal get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof PortalPrincipal principal)) {
            throw ServicingException.forbidden("No portal session");
        }
        if (principal.borrowerId() == null) {
            throw ServicingException.forbidden("Portal account is not linked to a borrower");
        }
        return principal;
    }

    public Long borrowerId() {
        return get().borrowerId();
    }
}
