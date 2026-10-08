package com.example.lending.loan.borrower;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BorrowerProfileService {

    private final BorrowerProfileRepository profileRepository;

    public BorrowerProfileService(BorrowerProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public BorrowerProfile profileFor(Long borrowerId) {
        return profileRepository.findByBorrowerId(borrowerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
    }

    @Transactional
    public BorrowerProfile replaceProfile(Long borrowerId, BorrowerProfile incoming) {
        BorrowerProfile current = profileFor(borrowerId);
        incoming.setId(current.getId());
        incoming.setBorrowerId(borrowerId);
        return profileRepository.save(incoming);
    }
}
