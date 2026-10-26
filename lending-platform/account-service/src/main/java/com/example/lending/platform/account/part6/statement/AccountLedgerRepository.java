package com.example.lending.platform.account.part6.statement;

import com.example.lending.platform.account.entity.Ledger;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountLedgerRepository extends JpaRepository<Ledger, Long> {

    List<Ledger> findByAccountIdOrderByIdAsc(Long accountId);
}
