package com.example.lending.loan.scheduling;

import com.example.lending.loan.scheduling.Jobs.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;

/** Fire de-duplication so each job fire runs on at most one instance. */
@Component
public class JobProtectionSupport {

    public record ProtectionLock(boolean isAcquired) {
    }

    private final JdbcTemplate jdbc;
    private final boolean protectionEnabled;

    public JobProtectionSupport(JdbcTemplate jdbc, @Value("${scheduling.protection.enabled:true}") boolean protectionEnabled) {
        this.jdbc = jdbc;
        this.protectionEnabled = protectionEnabled;
    }

    public boolean isProtectionEnabled() {
        return protectionEnabled;
    }

    public ExecutionMetadata buildExecutionMetadata(JobExecutionContext context, JobTrigger trigger) {
        ScheduledJob job = (ScheduledJob) context.getMergedJobDataMap().get(JobDataKey.SCHEDULE_JOB_KEY.getType());
        return new ExecutionMetadata(job.jobName(), trigger.scheduledFireTime(), trigger.fireTime());
    }

    public ProtectionLock tryAcquireFireDedupLock(ScheduledJob job, ExecutionMetadata metadata) {
        int inserted = jdbc.update("INSERT INTO lending.job_fire_locks (job_name, scheduled_fire_time) VALUES (?, ?) "
                + "ON CONFLICT (job_name, scheduled_fire_time) DO NOTHING",
                job.jobName(), Timestamp.from(metadata.scheduledFireTime()));
        return new ProtectionLock(inserted == 1);
    }
}
