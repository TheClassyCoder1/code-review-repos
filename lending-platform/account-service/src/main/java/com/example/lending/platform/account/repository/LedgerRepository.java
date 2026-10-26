package com.example.lending.platform.account.repository;

import com.example.lending.platform.account.entity.Ledger;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LedgerRepository extends JpaRepository<Ledger, Long> {

    /** Keyset page of ledger lines for one account, oldest first. */
    @Query("select l from Ledger l where l.accountId = :accountId and l.id > :afterId order by l.id asc")
    List<Ledger> pageForAccount(@Param("accountId") Long accountId, @Param("afterId") Long afterId, Pageable page);

    @Query("select coalesce(sum(l.entry), 0) from Ledger l where l.accountId = :accountId and l.id <= :uptoId")
    double balanceUpTo(@Param("accountId") Long accountId, @Param("uptoId") Long uptoId);
}
