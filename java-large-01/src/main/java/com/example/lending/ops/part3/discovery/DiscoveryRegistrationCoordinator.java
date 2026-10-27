package com.example.lending.ops.part3.discovery;

import com.example.lending.ops.part3.discovery.DiscoveryModel.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Entry point for upstream registrations. Takes a transaction-scoped advisory lock per plugin and namespace, then runs the binding in that same transaction, so look-up-then-insert steps for one plugin never interleave across console instances; the lock is released when the transaction commits. */
@Component
public class DiscoveryRegistrationCoordinator {
    private final TransactionTemplate transactionTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final DiscoveryServiceImpl discoveryService;
    public DiscoveryRegistrationCoordinator(PlatformTransactionManager tm, JdbcTemplate jdbcTemplate, DiscoveryServiceImpl discoveryService) {
        this.transactionTemplate = new TransactionTemplate(tm);
        this.jdbcTemplate = jdbcTemplate;
        this.discoveryService = discoveryService;
    }
    public void register(DiscoveryConfigRegisterDTO dto, SelectorDO selector) {
        long lockKey = ("discovery|" + dto.getPluginName() + "|" + dto.getNamespaceId()).hashCode();
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.query("SELECT pg_advisory_xact_lock(?)", (ResultSetExtractor<Void>) rs -> null, lockKey);
            discoveryService.registerDiscoveryConfig(dto, selector);
        });
    }
}
