package com.example.lending.loan.servicing.collections.documents;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdentityDocumentScanRepository extends JpaRepository<IdentityDocumentScan, Long> {

    boolean existsByCardNumberAndIdentifier(String cardNumber, String identifier);

    Optional<IdentityDocumentScan> findByCardNumberAndIdentifier(String cardNumber, String identifier);
}
