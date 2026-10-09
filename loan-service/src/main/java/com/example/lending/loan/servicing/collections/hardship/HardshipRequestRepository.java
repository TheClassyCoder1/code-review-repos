package com.example.lending.loan.servicing.collections.hardship;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HardshipRequestRepository extends JpaRepository<HardshipRequest, Long> {

    List<HardshipRequest> findByBorrowerIdOrderByCreatedAtDesc(Long borrowerId);
}
