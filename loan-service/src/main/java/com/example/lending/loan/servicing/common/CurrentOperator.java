package com.example.lending.loan.servicing.common;

import com.example.lending.loan.servicing.security.OperatorPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Access to the operator bound to the current request. */
@Component
public class CurrentOperator {

    public OperatorPrincipal get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof OperatorPrincipal principal)) {
            throw ServicingException.forbidden("No operator session");
        }
        return principal;
    }

    public Long id() {
        return get().getId();
    }

    public Long idOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof OperatorPrincipal principal) {
            return principal.getId();
        }
        return null;
    }

    public String tenantId() {
        return get().getTenantId();
    }
}
