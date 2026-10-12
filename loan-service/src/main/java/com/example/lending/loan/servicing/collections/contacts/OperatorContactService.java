package com.example.lending.loan.servicing.collections.contacts;

import com.example.lending.loan.servicing.common.CurrentOperator;
import com.example.lending.loan.servicing.common.ServicingMailer;
import com.example.lending.loan.servicing.common.ServicingException;
import com.example.lending.loan.servicing.security.OperatorAccount;
import com.example.lending.loan.servicing.security.OperatorAccountRepository;
import org.springframework.stereotype.Service;

/** Team directory requests between collections operators. */
@Service
public class OperatorContactService {

    static final class ContactConfirmationTemplate {

        private ContactConfirmationTemplate() {
        }

        static String getEmail(OperatorContact contact) {
            return "Hello " + contact.getOwner().getDisplayName() + ",\n\n"
                    + contact.getContact().getDisplayName()
                    + " accepted your contact request. You can now share collections cases with each other.\n";
        }
    }

    private static final String ACCEPTED_SUFFIX = "accepted your contact request";

    private final OperatorContactDao dao;
    private final OperatorContactRepository contacts;
    private final PrivateMessageDao privateMessageDao;
    private final ServicingMailer mailer;
    private final CurrentOperator currentOperator;
    private final OperatorAccountRepository operators;

    public OperatorContactService(OperatorContactDao dao, OperatorContactRepository contacts,
                                  PrivateMessageDao privateMessageDao, ServicingMailer mailer,
                                  CurrentOperator currentOperator, OperatorAccountRepository operators) {
        this.operators = operators;
        this.dao = dao;
        this.contacts = contacts;
        this.privateMessageDao = privateMessageDao;
        this.mailer = mailer;
        this.currentOperator = currentOperator;
    }

    public OperatorContact find(long userContactId) {
        return dao.get(userContactId);
    }

    public OperatorContact requestContact(long contactId) {
        OperatorAccount target = operators.findById(contactId)
                .orElseThrow(() -> ServicingException.notFound("Operator " + contactId + " not found"));
        if (!target.getTenantId().equals(currentOperator.tenantId())) {
            throw ServicingException.notFound("Operator " + contactId + " not found");
        }
        return dao.add(currentOperator.id(), contactId, true);
    }


    public Object acceptUserContact(long userContactId) {
        OperatorContact contact = dao.get(userContactId);

        if (contact == null) {
            return "error.contact.denied";
        }

        if (!contact.isPending()) {
            return "error.contact.approved";
        }

        dao.updateContactStatus(userContactId, false);

        contact = dao.get(userContactId);
        OperatorAccount user = contact.getOwner();

        dao.add(user.getId(), currentOperator.id(), false);

        if (user.getEmail() != null) {
            String message = ContactConfirmationTemplate.getEmail(contact);

            String subj = contact.getContact().getDisplayName() + " " + ACCEPTED_SUFFIX;

            privateMessageDao.addPrivateMessage(subj, message, contact.getContact(), user, user);

            mailer.send(user.getEmail(), subj, message);
        }
        return userContactId;
    }
}
