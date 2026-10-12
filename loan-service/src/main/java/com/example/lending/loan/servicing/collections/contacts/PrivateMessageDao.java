package com.example.lending.loan.servicing.collections.contacts;

import com.example.lending.loan.servicing.security.OperatorAccount;
import org.springframework.data.jpa.repository.JpaRepository;

/** Inbox messages of operators. */
public interface PrivateMessageDao extends JpaRepository<PrivateMessage, Long> {

    default PrivateMessage addPrivateMessage(String subject, String body, OperatorAccount from,
                                             OperatorAccount to, OperatorAccount owner) {
        PrivateMessage message = new PrivateMessage();
        message.setSubject(subject);
        message.setBody(body);
        message.setFromOperatorId(from.getId());
        message.setToOperatorId(to.getId());
        message.setOwnerId(owner.getId());
        return save(message);
    }
}
