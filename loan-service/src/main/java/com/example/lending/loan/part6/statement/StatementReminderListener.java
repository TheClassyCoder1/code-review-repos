package com.example.lending.loan.part6.statement;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Queues a payment reminder for every closed statement that still has a balance owing.
 * Only the latest reminder per loan is kept; the reminder dispatcher drains the queue.
 */
@Component
public class StatementReminderListener {

    private static final Logger log = LoggerFactory.getLogger(StatementReminderListener.class);

    private final Map<Long, StatementClosedEvent> pendingReminders = new ConcurrentHashMap<>();

    @EventListener(condition = "!#event.isPaidOff()")
    public void onStatementClosed(StatementClosedEvent event) {
        pendingReminders.put(event.getLoanId(), event);
        log.info("payment reminder queued for loan {} due {}", event.getLoanId(), event.getDueDate());
    }

    public Map<Long, StatementClosedEvent> drain() {
        Map<Long, StatementClosedEvent> snapshot = Map.copyOf(pendingReminders);
        snapshot.forEach(pendingReminders::remove);
        return snapshot;
    }
}
