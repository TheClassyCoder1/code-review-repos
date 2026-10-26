package com.example.lending.loan.part6.kyc;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KycProfileRepository extends JpaRepository<KycProfile, Long> {

    Optional<KycProfile> findByReference(String reference);

    boolean existsByNationalIdAndUserIdNot(String nationalId, Long userId);
}
