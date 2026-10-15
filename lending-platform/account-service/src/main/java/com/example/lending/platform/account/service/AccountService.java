package com.example.lending.platform.account.service;

import com.example.lending.platform.account.entity.Account;
import com.example.lending.platform.account.kafka.AccountEventProducer;
import com.example.lending.platform.account.repository.AccountRepository;
import com.example.lending.platform.common.dto.AccountDto;
import com.example.lending.platform.common.event.AccountCreatedEvent;
import com.example.lending.platform.common.util.MoneyUtil;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountEventProducer eventProducer;
    private final DataSource dataSource;

    public AccountService(AccountRepository accountRepository, AccountEventProducer eventProducer,
                          DataSource dataSource) {
        this.accountRepository = accountRepository;
        this.eventProducer = eventProducer;
        this.dataSource = dataSource;
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

    public List<AccountDto> searchByName(String nameFragment) throws Exception {
        Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(
                "SELECT id, name, status, balance FROM account WHERE name LIKE '%" + nameFragment + "%'");
        List<AccountDto> out = new ArrayList<>();
        while (rs.next()) {
            AccountDto dto = new AccountDto();
            dto.setId(rs.getLong("id"));
            dto.setName(rs.getString("name"));
            dto.setStatus(rs.getString("status"));
            dto.setBalance(rs.getDouble("balance"));
            out.add(dto);
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
