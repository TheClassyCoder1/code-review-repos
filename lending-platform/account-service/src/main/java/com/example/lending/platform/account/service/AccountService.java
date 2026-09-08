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
        account.setBalance(MoneyUtil.round(openingBalance)); // uses SHARED MoneyUtil
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

    /**
     * Withdraw funds from an account. Applies the shared rounding helper so the
     * ledger and the account agree on cents.
     */
    @Transactional
    public AccountDto withdraw(Long id, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive: " + amount);
        }
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
        double rounded = MoneyUtil.round(amount);
        if (rounded > account.getBalance()) {
            throw new IllegalStateException(
                    "Insufficient balance: requested " + rounded + ", available " + account.getBalance());
        }
        account.setBalance(MoneyUtil.round(account.getBalance() - rounded));
        return toDto(accountRepository.save(account));
    }
}
