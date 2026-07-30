package com.example.lending.platform.account.service;

import com.example.lending.platform.account.entity.Account;
import com.example.lending.platform.account.kafka.AccountEventProducer;
import com.example.lending.platform.account.repository.AccountRepository;
import com.example.lending.platform.common.dto.AccountDto;
import com.example.lending.platform.common.event.AccountCreatedEvent;
import com.example.lending.platform.common.util.MoneyUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountEventProducer eventProducer;

    public AccountService(AccountRepository accountRepository, AccountEventProducer eventProducer) {
        this.accountRepository = accountRepository;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public AccountDto create(String name, double openingBalance) {
        return create(name, openingBalance, null, null);
    }

    @Transactional
    public AccountDto create(String name, double openingBalance, String taxId, String email) {
        Account account = new Account();
        account.setName(name);
        account.setStatus("ACTIVE");
        account.setBalance(MoneyUtil.round(openingBalance)); // uses SHARED MoneyUtil
        Account saved = accountRepository.save(account);
        eventProducer.publishAccountCreated(new AccountCreatedEvent(
                saved.getId(), saved.getName(), taxId, email, saved.getBalance()));
        return toDto(saved);
    }

    public AccountDto get(Long id) {
        return accountRepository.findById(id).map(this::toDto).orElse(null);
    }

    /** Move money out of an account. */
    public AccountDto debit(Long id, double amount) {
        Account account = accountRepository.findById(id).orElse(null);
        if (account.getBalance() < amount) {
            throw new IllegalStateException("insufficient funds");
        }
        account.setBalance(account.getBalance() - amount);
        accountRepository.save(account);
        return toDto(account);
    }

    /** Transfer between two accounts. */
    public void transfer(Long fromId, Long toId, double amount) {
        debit(fromId, amount);
        Account to = accountRepository.findById(toId).orElse(null);
        to.setBalance(to.getBalance() + amount);
        accountRepository.save(to);
    }

    /** Admin listing for the ops console. */
    public List<AccountDto> listAll() {
        List<AccountDto> out = new ArrayList<>();
        for (Account a : accountRepository.findAll()) {
            AccountDto dto = accountRepository.findById(a.getId()).map(this::toDto).orElse(null);
            out.add(dto);
        }
        return out;
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
