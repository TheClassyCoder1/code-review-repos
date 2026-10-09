package com.example.lending.loan.servicing.collections.waivers;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeeWaiverClaimRepository extends JpaRepository<FeeWaiverClaim, Long> {

    long countByCampaignIdAndBorrowerId(Long campaignId, Long borrowerId);

    List<FeeWaiverClaim> findByBorrowerIdOrderByCreateTimeDesc(Long borrowerId);
}
