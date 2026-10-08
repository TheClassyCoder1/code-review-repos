package com.example.lending.loan.auth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountTokenRepository extends JpaRepository<AccountToken, String> {
}
