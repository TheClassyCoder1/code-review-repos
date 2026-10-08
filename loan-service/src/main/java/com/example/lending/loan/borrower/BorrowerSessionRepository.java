package com.example.lending.loan.borrower;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BorrowerSessionRepository extends JpaRepository<BorrowerSession, String> {

    void deleteByBorrowerId(Long borrowerId);
}
