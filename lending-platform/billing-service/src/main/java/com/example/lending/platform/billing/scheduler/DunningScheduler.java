package com.example.lending.platform.billing.scheduler;

import com.example.lending.platform.billing.service.DunningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Triggers the collections pass every morning before the borrower-facing channels open.
 * Can be switched off per environment with {@code billing.dunning.enabled=false}.
 */
@Component
public class DunningScheduler {

    private static final Logger log = LoggerFactory.getLogger(DunningScheduler.class);

    @Autowired
    private DunningService dunningService;

    @Value("${billing.dunning.enabled:true}")
    private boolean enabled;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(cron = "${billing.dunning.cron:0 30 6 * * *}", zone = "${billing.zone:Europe/Amsterdam}")
    public void runDaily() {
        if (!enabled) {
            log.debug("dunning disabled; skipping scheduled run");
            return;
        }
        if (!running.compareAndSet(false, true)) {
            log.warn("previous dunning run still in progress; skipping");
            return;
        }
        try {
            DunningService.DunningRun run = dunningService.run();
            log.info("scheduled dunning finished: {} reminders sent", run.getRemindersSent());
        } catch (RuntimeException e) {
            log.error("scheduled dunning failed", e);
        } finally {
            running.set(false);
        }
    }
}
