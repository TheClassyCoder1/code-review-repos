package com.example.lending.loan.servicing.admin.imports;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OperatorCredentialRepository extends JpaRepository<OperatorCredential, String> {

    Optional<OperatorCredential> findByIdAndOperatorId(String id, Long operatorId);
}
