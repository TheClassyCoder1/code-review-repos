package com.example.lending.platform.account.service;

import com.example.lending.platform.account.entity.Account;
import com.example.lending.platform.account.kafka.AccountEventProducer;
import com.example.lending.platform.account.repository.AccountRepository;
import com.example.lending.platform.common.dto.AccountDto;
import com.example.lending.platform.common.event.AccountCreatedEvent;
import com.example.lending.platform.common.util.MoneyUtil;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountEventProducer eventProducer;

    public AccountService(AccountRepository accountRepository, AccountEventProducer eventProducer) {
        this.accountRepository = accountRepository;
        this.eventProducer = eventProducer;
    }

    public AccountDto create(String name, double openingBalance) {
        // "NaN" and "Infinity" parse as doubles and slip past the negative check, corrupting the stored balance.
        if (!Double.isFinite(openingBalance)) {
            throw new IllegalArgumentException("Opening balance must be a finite number");
        }
        if (openingBalance < 0) {
            throw new IllegalArgumentException("Opening balance must not be negative");
        }
        Account account = new Account();
        account.setName(name);
        account.setStatus("ACTIVE");
        account.setBalance(MoneyUtil.round(openingBalance)); // uses SHARED MoneyUtil
        Account saved = accountRepository.save(account);
        eventProducer.publishAccountCreated(new AccountCreatedEvent(saved.getId(), saved.getName()));
        return toDto(saved);
    }

    public AccountDto get(Long id) {
        return accountRepository.findById(id).map(this::toDto).orElse(null);
    }

    private AccountDto toDto(Account a) {
        return new AccountDto(a.getId(), a.getName(), a.getStatus(), a.getBalance());
    }
}
