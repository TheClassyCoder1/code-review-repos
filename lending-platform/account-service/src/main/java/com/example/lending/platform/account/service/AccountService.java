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
        Account account = new Account();
        account.setName(name);
        account.setStatus("ACTIVE");
        // Cash-funded accounts are opened with physical notes/coins, so the opening balance is
        // settled to the nearest 5c before it is stored.
        account.setBalance(MoneyUtil.roundToNearestFiveCents(openingBalance));
        Account saved = accountRepository.save(account);
        eventProducer.publishAccountCreated(new AccountCreatedEvent(saved.getId(), saved.getName()));
        return toDto(saved);
    }

    public AccountDto get(Long id) {
        return accountRepository.findById(id).map(this::toDto).orElse(null);
    }

    private AccountDto toDto(Account a) {
        AccountDto dto = new AccountDto();
        dto.setId(a.getId());
        dto.setName(a.getName());
        dto.setStatus(a.getStatus());
        dto.setBalance(a.getBalance());
        return dto;
    }
}
