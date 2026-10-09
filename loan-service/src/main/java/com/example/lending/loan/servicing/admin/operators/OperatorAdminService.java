package com.example.lending.loan.servicing.admin.operators;

import com.example.lending.loan.servicing.common.ServicingException;
import com.example.lending.loan.servicing.security.OperatorAccount;
import com.example.lending.loan.servicing.security.OperatorAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Operator administration. */
@Service
public class OperatorAdminService {

    private final OperatorAccountRepository operators;

    public OperatorAdminService(OperatorAccountRepository operators) {
        this.operators = operators;
    }

    @Transactional(readOnly = true)
    public List<OperatorAccount> listUsers(String tenantId) {
        return operators.findByTenantIdOrderByUsername(tenantId);
    }

    @Transactional
    public void deleteUser(String tenantId, Long userId) {
        OperatorAccount account = operators.findById(userId)
                .filter(found -> found.getTenantId().equals(tenantId))
                .orElseThrow(() -> ServicingException.notFound("Operator " + userId + " not found"));
        operators.delete(account);
    }
}
