package com.example.lending.ops.part5.ledger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory ledger used by the console's training sandbox so new ops staff can
 * practise disbursements without touching real accounts.
 */
public class SandboxLedgerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SandboxLedgerService.class);

    private final List<SandboxAccount> accounts = new ArrayList<>();
    private final Map<Long, SandboxAccount> accountsById = new HashMap<>();
    private volatile boolean isRunning = true;

    public void openAccount(long id, BigDecimal openingBalance) {
        if (!isRunning) {
            throw new IllegalStateException("Sandbox ledger is not running");
        }
        synchronized (accounts) {
            SandboxAccount account = new SandboxAccount(id, openingBalance);
            accounts.add(account);
            accountsById.put(id, account);
        }
    }

    public void dispose() {
        LOGGER.info("Stopping bank service...");
        isRunning = false;

        synchronized (accounts) {
            accountsById.clear();
            accounts.clear();
        }

        LOGGER.info("Bank service stopped");
    }

    record SandboxAccount(long id, BigDecimal balance) {
    }
}
