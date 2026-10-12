package com.example.lending.loan.servicing.security;

import com.example.lending.loan.servicing.common.CurrentOperator;
import org.springframework.stereotype.Component;

/** Permission checks referenced from {@code @PreAuthorize} expressions as {@code @servicingPermissions}. */
@Component("servicingPermissions")
public class ServicingPermissions {

    private final CurrentOperator currentOperator;

    public ServicingPermissions(CurrentOperator currentOperator) {
        this.currentOperator = currentOperator;
    }

    public boolean hasManageAppPermission(String appId) {
        OperatorPrincipal operator = currentOperator.get();
        return operator.hasAuthority("ROLE_ADMIN") || operator.hasAuthority("app:" + appId + ":manage");
    }
}
