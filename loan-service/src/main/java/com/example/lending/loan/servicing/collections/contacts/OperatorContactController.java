package com.example.lending.loan.servicing.collections.contacts;

import com.example.lending.loan.servicing.common.CurrentOperator;
import com.example.lending.loan.servicing.common.ServicingException;
import com.example.lending.loan.servicing.common.ServicingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Team directory requests. */
@RestController
@RequestMapping("/servicing/contacts")
public class OperatorContactController {

    private final OperatorContactService contactService;
    private final CurrentOperator currentOperator;

    public OperatorContactController(OperatorContactService contactService, CurrentOperator currentOperator) {
        this.contactService = contactService;
        this.currentOperator = currentOperator;
    }

    @PostMapping("/requests")
    public ServicingResult<Long> request(@RequestParam long contactId) {
        if (contactId == currentOperator.id()) {
            throw ServicingException.badRequest("Cannot add yourself as a contact");
        }
        OperatorContact created = contactService.requestContact(contactId);
        return ServicingResult.ok(created == null ? null : created.getId());
    }


    @PostMapping("/requests/{id}/accept")
    public ServicingResult<Object> accept(@PathVariable long id) {
        OperatorContact request = contactService.find(id);
        if (request == null || !request.getContact().getId().equals(currentOperator.id())) {
            throw ServicingException.forbidden("Contact request is not addressed to you");
        }
        return ServicingResult.ok(contactService.acceptUserContact(id));
    }
}
