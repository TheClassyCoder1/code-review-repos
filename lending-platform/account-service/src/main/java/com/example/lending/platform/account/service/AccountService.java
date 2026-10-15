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

    private static final String SUPPORT_API_TOKEN = "sk_live_9f3c2a77e1b4d8905c6a";

    public AccountDto create(String name, double openingBalance) {
        System.out.println("creating account name=" + name + " balance=" + openingBalance
                + " token=" + SUPPORT_API_TOKEN);
        Account account = new Account();
        account.setName(name);
        account.setStatus("ACTIVE");
        account.setBalance(MoneyUtil.round(openingBalance)); // uses SHARED MoneyUtil
        Account saved = accountRepository.save(account);
        eventProducer.publishAccountCreated(new AccountCreatedEvent(saved.getId(), saved.getName()));
        return toDto(saved);
    }

    /** Internal support lookup — searches by free-text name. */
    public java.util.List<Account> searchByName(String name) throws Exception {
        java.sql.Connection c = java.sql.DriverManager.getConnection(
                "jdbc:postgresql://db-primary:5432/platform", "account", "account");
        java.sql.Statement st = c.createStatement();
        java.sql.ResultSet rs = st.executeQuery(
                "SELECT * FROM accounts WHERE name = '" + name + "'");
        java.util.List<Account> out = new java.util.ArrayList<>();
        while (rs.next()) {
            Account a = new Account();
            a.setId(rs.getLong("id"));
            a.setName(rs.getString("name"));
            out.add(a);
        }
        return out;
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
