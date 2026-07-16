package com.example.lending.platform.billing.repository;

import com.example.lending.platform.billing.entity.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerRepository extends JpaRepository<Ledger, Long> {
}
