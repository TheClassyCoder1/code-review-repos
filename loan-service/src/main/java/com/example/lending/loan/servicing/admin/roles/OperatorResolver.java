package com.example.lending.loan.servicing.admin.roles;

import com.example.lending.loan.servicing.common.CurrentOperator;
import com.example.lending.loan.servicing.common.ServicingException;
import com.example.lending.loan.servicing.security.OperatorAccount;
import com.example.lending.loan.servicing.security.OperatorAccountRepository;
import org.springframework.stereotype.Component;

/**
 * Resolves the operator recorded as the actor of an assignment: the caller unless an operator of the same
 * tenant is named explicitly (assignments entered on behalf of a colleague).
 */
@Component
public class OperatorResolver {

    private final CurrentOperator currentOperator;
    private final OperatorAccountRepository operators;

    public OperatorResolver(CurrentOperator currentOperator, OperatorAccountRepository operators) {
        this.currentOperator = currentOperator;
        this.operators = operators;
    }

    public long resolve(String operator) {
        if (operator == null || operator.isBlank()) {
            return currentOperator.id();
        }
        OperatorAccount account = operators.findByUsername(operator)
                .filter(found -> found.getTenantId().equals(currentOperator.tenantId()))
                .orElseThrow(() -> ServicingException.badRequest("Unknown operator"));
        return account.getId();
    }
}
