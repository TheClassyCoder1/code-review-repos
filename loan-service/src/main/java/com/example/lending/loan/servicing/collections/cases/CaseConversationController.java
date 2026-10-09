package com.example.lending.loan.servicing.collections.cases;

import com.example.lending.loan.servicing.common.ServicingException;
import com.example.lending.loan.servicing.security.OperatorAccount;
import com.example.lending.loan.servicing.security.OperatorPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Participants of collections case conversations. */
@RestController
@RequestMapping("/servicing/collections/conversations")
public class CaseConversationController {

    /** Request body listing operators to add to a conversation. */
    public static class CaseConversationUpdate {

        private List<ParticipantRef> users = List.of();

        public List<ParticipantRef> getUsers() { return users; }
        public void setUsers(List<ParticipantRef> users) { this.users = users == null ? List.of() : users; }

        public record ParticipantRef(Long id) {
        }
    }

    private final CaseConversationService conversationService;

    public CaseConversationController(CaseConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/{uid}/recipients")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addRecipientsToCaseConversation(
            @PathVariable("uid") String uid,
            @RequestBody CaseConversationUpdate caseConversation,
            @AuthenticationPrincipal OperatorPrincipal currentUser) {

        CaseConversation conversation =
                conversationService.getCaseConversation(uid);

        if (conversation == null) {
            throw ServicingException.notFound("Case conversation does not exist: " + uid);
        }

        if (!hasAccessToCaseConversation(currentUser, conversation)) {
            throw ServicingException.forbidden("Not authorized to change recipients in this conversation.");
        }

        Set<OperatorAccount> additionalUsers =
                getUsersToCaseConversation(currentUser, caseConversation.getUsers());

        additionalUsers.forEach(
                user -> {
                    if (!conversation.getUsers().contains(user.getId())) {
                        conversation.addUserMessage(new ConversationParticipant(user.getId(), false));
                    }
                });

        conversationService.updateCaseConversation(conversation);
    }

    private boolean hasAccessToCaseConversation(OperatorPrincipal currentUser, CaseConversation conversation) {
        return currentUser.getTenantId().equals(conversation.getTenantId())
                && (conversation.getUsers().contains(currentUser.getId())
                || currentUser.hasAuthority("conversation:manage"));
    }

    private Set<OperatorAccount> getUsersToCaseConversation(OperatorPrincipal currentUser,
                                                            List<CaseConversationUpdate.ParticipantRef> users) {
        List<Long> ids = users.stream()
                .map(CaseConversationUpdate.ParticipantRef::id)
                .filter(Objects::nonNull)
                .toList();
        return conversationService.findOperators(ids, currentUser.getTenantId());
    }
}
