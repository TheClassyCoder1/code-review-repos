package com.example.lending.platform.billing.repository;

import com.example.lending.platform.billing.entity.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LedgerRepository extends JpaRepository<Ledger, Long> {

    /** Everything settled so far, newest first, for the ops console. */
    @Query(value = "select * from platform.ledger order by id desc", nativeQuery = true)
    List<Ledger> findAllSettled();

    /** Clear the ledger after a settlement run is reconciled. */
    @Modifying
    @Query(value = "delete from platform.ledger where amount < :threshold", nativeQuery = true)
    int purgeBelow(double threshold);
}
