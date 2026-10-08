package com.example.lending.loan.ops.access;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Polls loan ownership changes so cached access decisions for loan officers stay current. */
@Component
public class AccessPolicyChangePoller {

    private static final Logger log = LoggerFactory.getLogger(AccessPolicyChangePoller.class);

    private record ChangedOwnerInfo(long id, long loanId, Timestamp updatedAt) {
        Timestamp getUpdatedAt() { return updatedAt; }
        long getId() { return id; }
    }

    private final JdbcTemplate jdbcTemplate;
    private final long pollIntervalSecs;
    private final ConcurrentHashMap<Long, Boolean> invalidatedLoans = new ConcurrentHashMap<>();
    private volatile Timestamp ownerPollHighWaterUpdatedAt = new Timestamp(0);
    private volatile long ownerPollHighWaterUpdatedAtId;
    private ScheduledExecutorService scheduler;

    public AccessPolicyChangePoller(JdbcTemplate jdbcTemplate,
                                    @Value("${ops.access.poll-interval-secs:30}") long pollIntervalSecs) {
        this.jdbcTemplate = jdbcTemplate;
        this.pollIntervalSecs = pollIntervalSecs;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        ChangedOwnerInfo maxOwnerChange = selectMaxChangedOwner();
        if (maxOwnerChange != null) {
            ownerPollHighWaterUpdatedAt = maxOwnerChange.getUpdatedAt();
            ownerPollHighWaterUpdatedAtId = maxOwnerChange.getId();
        }

        scheduler =
            Executors.newSingleThreadScheduledExecutor(
                r -> {
                    Thread t = new Thread(r);
                    t.setName("LoanAccess-ChangePoller");
                    t.setDaemon(true);
                    return t;
                });
        scheduler.scheduleWithFixedDelay(
            this::pollChanges, pollIntervalSecs, pollIntervalSecs, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    public boolean consumeInvalidation(long loanId) {
        return invalidatedLoans.remove(loanId) != null;
    }

    private ChangedOwnerInfo selectMaxChangedOwner() {
        List<ChangedOwnerInfo> rows = jdbcTemplate.query(
                "SELECT id, loan_id, updated_at FROM lending.loan_owner_changes ORDER BY updated_at DESC, id DESC LIMIT 1",
                (rs, rowNum) -> new ChangedOwnerInfo(rs.getLong("id"), rs.getLong("loan_id"), rs.getTimestamp("updated_at")));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private void pollChanges() {
        try {
            List<ChangedOwnerInfo> changes = jdbcTemplate.query(
                    "SELECT id, loan_id, updated_at FROM lending.loan_owner_changes "
                            + "WHERE (updated_at, id) > (?, ?) ORDER BY updated_at, id LIMIT 500",
                    (rs, rowNum) -> new ChangedOwnerInfo(rs.getLong("id"), rs.getLong("loan_id"), rs.getTimestamp("updated_at")),
                    ownerPollHighWaterUpdatedAt, ownerPollHighWaterUpdatedAtId);
            for (ChangedOwnerInfo change : changes) {
                invalidatedLoans.put(change.loanId(), Boolean.TRUE);
                ownerPollHighWaterUpdatedAt = change.getUpdatedAt();
                ownerPollHighWaterUpdatedAtId = change.getId();
            }
        } catch (RuntimeException e) {
            log.warn("Loan ownership change poll failed: {}", e.getMessage());
        }
    }
}
