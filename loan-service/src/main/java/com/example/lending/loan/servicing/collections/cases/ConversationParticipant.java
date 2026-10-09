package com.example.lending.loan.servicing.collections.cases;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/** Operator taking part in a case conversation, with the read flag of the latest message. */
@Embeddable
public class ConversationParticipant {

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;
    @Column(name = "is_read", nullable = false)
    private boolean read;

    protected ConversationParticipant() {
    }

    public ConversationParticipant(Long operatorId, boolean read) {
        this.operatorId = operatorId;
        this.read = read;
    }

    public Long getOperatorId() { return operatorId; }
    public boolean isRead() { return read; }

    public void setRead(boolean read) { this.read = read; }

    @Override
    public boolean equals(Object o) {
        return o instanceof ConversationParticipant that && Objects.equals(operatorId, that.operatorId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(operatorId);
    }
}
