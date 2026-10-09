package com.example.lending.loan.servicing.collections.debtors;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DebtorRepository extends JpaRepository<Debtor, String> {
}
