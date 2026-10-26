package com.example.lending.loan.part7.topup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanTopUpRepository extends JpaRepository<LoanTopUp, Long> {

    List<LoanTopUp> findByLoanIdOrderByCreatedAtDesc(Long loanId);
}
