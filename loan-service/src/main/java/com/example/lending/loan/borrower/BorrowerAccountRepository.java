package com.example.lending.loan.borrower;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BorrowerAccountRepository extends JpaRepository<BorrowerAccount, Long> {

    Optional<BorrowerAccount> findByEmailIgnoreCase(String email);
}
