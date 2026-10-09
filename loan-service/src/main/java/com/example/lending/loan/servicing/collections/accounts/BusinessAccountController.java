package com.example.lending.loan.servicing.collections.accounts;

import com.example.lending.loan.servicing.collections.debtors.Debtor;
import com.example.lending.loan.servicing.collections.debtors.DebtorCommandGateway;
import com.example.lending.loan.servicing.collections.debtors.DebtorCommandGateway.ActivateDebtorCommand;
import com.example.lending.loan.servicing.collections.debtors.DebtorCommandGateway.CreateDebtorCommand;
import com.example.lending.loan.servicing.collections.debtors.DebtorService;
import com.example.lending.loan.servicing.common.ServicingException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/** Onboarding of business debtors placed with collections. */
@Controller
@RequestMapping("/servicing/collections/debtors")
public class BusinessAccountController {

    private final DebtorService debtorService;
    private final DebtorCommandGateway commandGateway;
    private final RepaymentAccountManager repaymentAccountManager;

    public BusinessAccountController(DebtorService debtorService, DebtorCommandGateway commandGateway,
                                     RepaymentAccountManager repaymentAccountManager) {
        this.debtorService = debtorService;
        this.commandGateway = commandGateway;
        this.repaymentAccountManager = repaymentAccountManager;
    }

    public static class BusinessDebtorRequest {

        @NotBlank
        @Pattern(regexp = "^[A-Z0-9-]{6,32}$")
        private String accountNumber;

        @NotBlank
        @Pattern(regexp = "^[a-z0-9-]{2,32}$")
        private String productIdentifier;

        private boolean active;

        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

        public String getProductIdentifier() { return productIdentifier; }
        public void setProductIdentifier(String productIdentifier) { this.productIdentifier = productIdentifier; }

        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    @PostMapping("/business")
    @PreAuthorize("hasAuthority('debtor:create')")
    @ResponseBody
    ResponseEntity<Void> createNonPerson(@RequestBody @Valid final BusinessDebtorRequest nonPerson) {
        Debtor debtor = new Debtor();
        debtor.setIdentifier(nonPerson.getAccountNumber());
        debtor.setType(Debtor.Type.BUSINESS.name());
        debtor.setCurrentState(Debtor.State.PENDING.name());
        debtor.setMember(false);
        if (this.debtorService.debtorExists(debtor.getIdentifier())) {
            throw ServicingException.conflict("Debtor " + debtor.getIdentifier() + " already exists.");
        }

        this.commandGateway.process(new CreateDebtorCommand(debtor));

        RepaymentAccountManager.AccountInstance accountInstance = new RepaymentAccountManager.AccountInstance();
        accountInstance.setProductIdentifier(nonPerson.getProductIdentifier());
        accountInstance.setDebtorIdentifier(debtor.getIdentifier());
        accountInstance.setAccountIdentifier(debtor.getIdentifier());
        repaymentAccountManager.create(accountInstance);
        if (nonPerson.isActive()) {
            this.commandGateway.process(new ActivateDebtorCommand(debtor.getIdentifier(), "ACTIVATE"));
            this.repaymentAccountManager.postAccountCommand(debtor.getIdentifier(), "ACTIVATE");
        }
        return ResponseEntity.accepted().build();
    }
}
