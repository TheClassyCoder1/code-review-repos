package com.example.lending.loan.servicing.statements.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StatementRepository extends JpaRepository<Statement, Long> {

    List<Statement> findByBorrowerIdAndStatusOrderByPeriodDesc(Long borrowerId, String status);

    Optional<Statement> findByIdAndBorrowerId(Long id, Long borrowerId);
}
