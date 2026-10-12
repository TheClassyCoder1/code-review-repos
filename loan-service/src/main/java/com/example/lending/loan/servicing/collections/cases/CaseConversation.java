package com.example.lending.loan.servicing.collections.cases;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Internal discussion thread attached to a collections case. */
@Entity
@Table(name = "servicing_case_conversations", schema = "lending")
public class CaseConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "uid", nullable = false, unique = true, updatable = false)
    private String uid;
    @Column(name = "case_id", nullable = false)
    private String caseId;
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "servicing_case_conversation_participants", schema = "lending",
            joinColumns = @JoinColumn(name = "conversation_id"))
    private Set<ConversationParticipant> participants = new HashSet<>();

    public Long getId() { return id; }
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Set<ConversationParticipant> getParticipants() { return participants; }

    /** Operator ids of all participants. */
    public Set<Long> getUsers() {
        return participants.stream().map(ConversationParticipant::getOperatorId).collect(Collectors.toSet());
    }

    public void addUserMessage(ConversationParticipant participant) {
        participants.add(participant);
    }
}
