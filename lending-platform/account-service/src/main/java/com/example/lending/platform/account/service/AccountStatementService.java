package com.example.lending.platform.account.service;

import com.example.lending.platform.account.dto.StatementLineDto;
import com.example.lending.platform.account.dto.StatementPageDto;
import com.example.lending.platform.account.entity.Ledger;
import com.example.lending.platform.account.repository.AccountRepository;
import com.example.lending.platform.account.repository.LedgerRepository;
import com.example.lending.platform.common.util.MoneyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds paged account statements from the ledger. Each line carries the balance after it
 * was posted so the portal can render a statement without summing on the client.
 */
@Service
public class AccountStatementService {

    static final int MAX_PAGE_SIZE = 200;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LedgerRepository ledgerRepository;

    /** Returns null when the account does not exist. */
    @Transactional(readOnly = true)
    public StatementPageDto page(Long accountId, Long afterId, int size) {
        if (!accountRepository.existsById(accountId)) {
            return null;
        }
        long cursor = afterId == null ? 0L : Math.max(0L, afterId);
        int limit = Math.max(1, Math.min(size, MAX_PAGE_SIZE));

        List<Ledger> rows = ledgerRepository.pageForAccount(accountId, cursor, PageRequest.of(0, limit + 1));
        boolean hasMore = rows.size() > limit;
        List<Ledger> visible = hasMore ? rows.subList(0, limit) : rows;

        double running = cursor == 0L ? 0d : ledgerRepository.balanceUpTo(accountId, cursor);
        List<StatementLineDto> lines = new ArrayList<>(visible.size());
        for (Ledger row : visible) {
            running = MoneyUtil.roundToCents(running + row.getEntry());
            lines.add(new StatementLineDto(row.getId(), row.getEntry(), running));
        }

        Long next = hasMore ? visible.get(visible.size() - 1).getId() : null;
        return new StatementPageDto(accountId, lines, next);
    }
}
