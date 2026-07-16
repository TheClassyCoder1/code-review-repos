package com.example.lending.platform.account.repository;

import com.example.lending.platform.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
}
