package com.example.lending.loan.servicing.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OperatorAccountRepository extends JpaRepository<OperatorAccount, Long> {

    Optional<OperatorAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    List<OperatorAccount> findByTenantIdOrderByUsername(String tenantId);
}
