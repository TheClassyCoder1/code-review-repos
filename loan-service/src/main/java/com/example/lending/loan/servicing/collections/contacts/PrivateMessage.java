package com.example.lending.loan.servicing.collections.contacts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Message in an operator's back-office inbox. */
@Entity
@Table(name = "servicing_private_messages", schema = "lending")
public class PrivateMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "subject", nullable = false)
    private String subject;
    @Column(name = "body", nullable = false, length = 8000)
    private String body;
    @Column(name = "from_operator_id", nullable = false)
    private Long fromOperatorId;
    @Column(name = "to_operator_id", nullable = false)
    private Long toOperatorId;
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Long getFromOperatorId() { return fromOperatorId; }
    public void setFromOperatorId(Long fromOperatorId) { this.fromOperatorId = fromOperatorId; }
    public Long getToOperatorId() { return toOperatorId; }
    public void setToOperatorId(Long toOperatorId) { this.toOperatorId = toOperatorId; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
}
