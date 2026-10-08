package com.example.lending.loan.scheduling;

import com.example.lending.loan.scheduling.Jobs.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Configuration
@EnableScheduling
public class NotificationJobScheduler {

    private final ScheduledJobExecutor executor;
    private final SchedulerLeaseManager leaseManager;

    public NotificationJobScheduler(ScheduledJobExecutor executor, SchedulerLeaseManager leaseManager) {
        this.executor = executor;
        this.leaseManager = leaseManager;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void dispatchNotices() {
        fire("notice-dispatch", ChronoUnit.MINUTES);
    }

    private void fire(String task, ChronoUnit unit) {
        if (!leaseManager.holdsLease()) {
            return;
        }
        Instant now = Instant.now();
        ScheduledJob job = new ScheduledJob(task, task);
        executor.execute(new JobExecutionContext(Map.of(JobDataKey.SCHEDULE_JOB_KEY.getType(), job),
                new JobTrigger(task, now.truncatedTo(unit), now)));
    }
}
