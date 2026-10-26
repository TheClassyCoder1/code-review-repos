package com.example.lending.platform.account.part6.statement;

import com.example.lending.platform.account.entity.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<Ledger, Long> {

    List<Ledger> findByAccountIdOrderByIdAsc(Long accountId);
}
