package com.example.lending.ops.part4.session;

import com.example.lending.ops.part4.scheduling.JobDetail;
import com.example.lending.ops.part4.scheduling.Scheduler;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * The only place the session validation job is scheduled. The session manager is required at construction and
 * is always put in the job data under {@link QuartzSessionValidationJob#SESSION_MANAGER_KEY}; the trigger
 * carries no data, so nothing can override it with an absent value.
 */
public class QuartzSessionValidationScheduler {

    private final Scheduler scheduler;
    private final ValidatingSessionManager sessionManager;

    public QuartzSessionValidationScheduler(Scheduler scheduler, ValidatingSessionManager sessionManager) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager is required for session validation");
    }

    public void enableSessionValidation(Duration interval) throws Exception {
        scheduler.scheduleJob(new JobDetail("SessionValidationJob", QuartzSessionValidationJob::new,
                Map.of(QuartzSessionValidationJob.SESSION_MANAGER_KEY, sessionManager)), interval, Map.of());
    }
}
