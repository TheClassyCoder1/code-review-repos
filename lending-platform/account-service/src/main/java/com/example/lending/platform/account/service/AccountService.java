package com.example.lending.platform.account.service;

import com.example.lending.platform.account.entity.Account;
import com.example.lending.platform.account.entity.Ledger;
import com.example.lending.platform.account.kafka.AccountEventProducer;
import com.example.lending.platform.account.repository.AccountRepository;
import com.example.lending.platform.account.repository.LedgerRepository;
import com.example.lending.platform.common.dto.AccountDto;
import com.example.lending.platform.common.event.AccountCreatedEvent;
import com.example.lending.platform.common.util.MoneyUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final LedgerRepository ledgerRepository;
    private final AccountEventProducer eventProducer;

    public AccountService(AccountRepository accountRepository, LedgerRepository ledgerRepository,
                          AccountEventProducer eventProducer) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public AccountDto create(String name, double openingBalance) {
        requireNonNegative(openingBalance, "Opening balance");
        Account account = new Account();
        account.setName(name);
        account.setStatus("ACTIVE");
        account.setBalance(MoneyUtil.roundToCents(openingBalance)); // uses SHARED MoneyUtil
        Account saved = accountRepository.save(account);
        if (saved.getBalance() != 0) {
            // The opening balance is the first statement line, so statements reconcile from zero.
            ledgerRepository.save(entry(saved.getId(), saved.getBalance()));
        }
        eventProducer.publishAccountCreated(new AccountCreatedEvent(saved.getId(), saved.getName()));
        return toDto(saved);
    }

    public AccountDto get(Long id) {
        return accountRepository.findById(id).map(this::toDto).orElse(null);
    }

    /**
     * Posts a manual adjustment (goodwill credit, fee reversal) to the account and its ledger.
     * Returns null when the account does not exist or is not active.
     */
    @Transactional
    public AccountDto adjust(Long id, double amount) {
        if (!Double.isFinite(amount) || amount == 0) {
            throw new IllegalArgumentException("Adjustment must be a non-zero finite amount");
        }
        Account account = accountRepository.findById(id).orElse(null);
        if (account == null || !"ACTIVE".equals(account.getStatus())) {
            return null;
        }
        double rounded = MoneyUtil.roundToCents(amount);
        double newBalance = MoneyUtil.roundToCents(account.getBalance() + rounded);
        requireNonNegative(newBalance, "Resulting balance");
        account.setBalance(newBalance);
        ledgerRepository.save(entry(account.getId(), rounded));
        return toDto(accountRepository.save(account));
    }

    private static void requireNonNegative(double amount, String label) {
        if (amount < 0) {
            throw new IllegalArgumentException(label + " must not be negative");
        }
    }

    private static Ledger entry(Long accountId, double amount) {
        Ledger ledger = new Ledger();
        ledger.setAccountId(accountId);
        ledger.setEntry(amount);
        return ledger;
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
