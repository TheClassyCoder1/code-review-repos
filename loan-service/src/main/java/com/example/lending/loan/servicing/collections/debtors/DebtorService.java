package com.example.lending.loan.servicing.collections.debtors;

import com.example.lending.loan.servicing.common.ServicingException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read access to debtors for the collections workflows. */
@Service
@Transactional(readOnly = true)
public class DebtorService {

    private final DebtorRepository repository;

    public DebtorService(DebtorRepository repository) {
        this.repository = repository;
    }

    public boolean debtorExists(String identifier) {
        return repository.existsById(identifier);
    }

    public Debtor getDebtor(String identifier) {
        return repository.findById(identifier)
                .orElseThrow(() -> ServicingException.notFound("Debtor " + identifier + " not found"));
    }

    public Address getAddress(String identifier) {
        AddressEntity address = getDebtor(identifier).getAddress();
        return address == null ? null : AddressMapper.map(address);
    }
}
