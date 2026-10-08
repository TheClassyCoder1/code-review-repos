package com.example.lending.loan.borrower;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BorrowerProfileRepository extends JpaRepository<BorrowerProfile, Long> {

    Optional<BorrowerProfile> findByBorrowerId(Long borrowerId);
}
