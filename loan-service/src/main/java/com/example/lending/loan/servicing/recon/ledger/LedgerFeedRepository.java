package com.example.lending.loan.servicing.recon.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerFeedRepository extends JpaRepository<LedgerFeed, Long> {

    List<LedgerFeed> findByEnabledTrue();
}
