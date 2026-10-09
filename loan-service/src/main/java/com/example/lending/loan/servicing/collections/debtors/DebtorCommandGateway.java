package com.example.lending.loan.servicing.collections.debtors;

import com.example.lending.loan.servicing.collections.documents.IdentityDocumentScan;
import com.example.lending.loan.servicing.common.ServicingException;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Applies debtor commands on the calling thread, each in its own transaction. */
@Component
public class DebtorCommandGateway {

    public record CreateDebtorCommand(Debtor debtor) {
    }

    public record ActivateDebtorCommand(String identifier, String comment) {
    }

    public record CreateIdentificationCardScanCommand(String number, IdentityDocumentScan scan, byte[] image,
                                                      String contentType) {
    }

    private final EntityManager entityManager;

    public DebtorCommandGateway(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void process(Object command) {
        if (command instanceof CreateDebtorCommand create) {
            entityManager.persist(create.debtor());
            entityManager.flush();
        } else if (command instanceof ActivateDebtorCommand activate) {
            Debtor debtor = entityManager.find(Debtor.class, activate.identifier());
            if (debtor == null || !Debtor.State.PENDING.name().equals(debtor.getCurrentState())) {
                throw ServicingException.conflict("Debtor " + activate.identifier() + " is not pending");
            }
            debtor.setCurrentState(Debtor.State.ACTIVE.name());
        } else if (command instanceof CreateIdentificationCardScanCommand createScan) {
            IdentityDocumentScan scan = createScan.scan();
            scan.setCardNumber(createScan.number());
            scan.setImage(createScan.image());
            scan.setContentType(createScan.contentType());
            entityManager.persist(scan);
            entityManager.flush();
        } else {
            throw new IllegalArgumentException("Unknown command " + command.getClass().getSimpleName());
        }
    }
}
