package com.example.lending.loan.partner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SyncTimeoutHandler {

    private static final Logger log = LoggerFactory.getLogger(SyncTimeoutHandler.class);

    public record SyncJob(Long id, String name, Long alertGroupId) {
    }

    public record SyncTimeoutEvent(long elapsedMs) {
    }

    public void handle(final SyncJob syncJob, final SyncTimeoutEvent syncTimeoutEvent) {
        final boolean shouldSendAlert = syncJob.alertGroupId() != null;

        if (shouldSendAlert) {
            doSyncTimeoutAlert(syncJob, syncTimeoutEvent);
        } else {
            log.info("Skipped sending timeout alert for sync job {} because alertGroupId is null.",
                    syncJob.name());
        }

    }

    private void doSyncTimeoutAlert(SyncJob syncJob, SyncTimeoutEvent syncTimeoutEvent) {
        log.warn("Sync job {} timed out after {} ms, alerting group {}",
                syncJob.id(), syncTimeoutEvent.elapsedMs(), syncJob.alertGroupId());
    }
}
