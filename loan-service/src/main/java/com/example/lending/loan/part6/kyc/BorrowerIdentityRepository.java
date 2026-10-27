package com.example.lending.loan.part6.kyc;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BorrowerIdentityRepository extends JpaRepository<BorrowerIdentity, Long> {

    Optional<BorrowerIdentity> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
