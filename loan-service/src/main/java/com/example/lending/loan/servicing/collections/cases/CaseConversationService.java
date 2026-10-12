package com.example.lending.loan.servicing.collections.cases;

import com.example.lending.loan.servicing.security.OperatorAccount;
import com.example.lending.loan.servicing.security.OperatorAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/** Case conversations and their participants. */
@Service
public class CaseConversationService {

    private final CaseConversationRepository conversations;
    private final OperatorAccountRepository operators;

    public CaseConversationService(CaseConversationRepository conversations, OperatorAccountRepository operators) {
        this.conversations = conversations;
        this.operators = operators;
    }

    @Transactional(readOnly = true)
    public CaseConversation getCaseConversation(String uid) {
        return conversations.findByUid(uid).orElse(null);
    }

    @Transactional
    public void updateCaseConversation(CaseConversation conversation) {
        conversations.save(conversation);
    }

    /** Operators with the given ids that belong to the tenant; unknown ids are ignored. */
    @Transactional(readOnly = true)
    public Set<OperatorAccount> findOperators(Collection<Long> ids, String tenantId) {
        return operators.findAllById(ids).stream()
                .filter(operator -> tenantId.equals(operator.getTenantId()))
                .collect(Collectors.toSet());
    }
}
