package com.example.lending.loan.servicing.collections.contacts;

import com.example.lending.loan.servicing.security.OperatorAccountRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Persistence operations on operator contacts. */
@Repository
public class OperatorContactDao {

    private final OperatorContactRepository contacts;
    private final OperatorAccountRepository operators;

    public OperatorContactDao(OperatorContactRepository contacts, OperatorAccountRepository operators) {
        this.contacts = contacts;
        this.operators = operators;
    }

    @Transactional(readOnly = true)
    public OperatorContact get(long id) {
        return contacts.findById(id).orElse(null);
    }

    @Transactional
    public void updateContactStatus(long id, boolean pending) {
        contacts.findById(id).ifPresent(contact -> contact.setPending(pending));
    }

    @Transactional
    public OperatorContact add(long ownerId, long contactId, boolean pending) {
        if (contacts.existsByOwnerIdAndContactId(ownerId, contactId)) {
            return null;
        }
        OperatorContact contact = new OperatorContact();
        contact.setOwner(operators.getReferenceById(ownerId));
        contact.setContact(operators.getReferenceById(contactId));
        contact.setPending(pending);
        return contacts.save(contact);
    }
}
