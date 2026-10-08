package com.example.lending.loan.scheduling;

import com.example.lending.loan.scheduling.Jobs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ScheduledJobExecutor {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobExecutor.class);

    private final JobProtectionSupport jobProtectionSupport;
    private final JobInvoker jobInvoker;

    public ScheduledJobExecutor(JobProtectionSupport jobProtectionSupport, JobInvoker jobInvoker) {
        this.jobProtectionSupport = jobProtectionSupport;
        this.jobInvoker = jobInvoker;
    }

    public void execute(JobExecutionContext jobExecutionContext) {
        ScheduledJob scheduledJob = (ScheduledJob) jobExecutionContext.getMergedJobDataMap()
            .get(JobDataKey.SCHEDULE_JOB_KEY.getType());
        ExecutionMetadata executionMetadata = jobProtectionSupport.buildExecutionMetadata(jobExecutionContext,
                jobExecutionContext.getTrigger());
        if (jobProtectionSupport.isProtectionEnabled()) {
            JobProtectionSupport.ProtectionLock protectionLock = jobProtectionSupport
                .tryAcquireFireDedupLock(scheduledJob, executionMetadata);
            if (!protectionLock.isAcquired()) {
                publishSkippedLog(scheduledJob, executionMetadata);
                return;
            }
        }
        jobInvoker.init(scheduledJob, jobExecutionContext.getTrigger(), executionMetadata);
    }

    private void publishSkippedLog(ScheduledJob job, ExecutionMetadata metadata) {
        log.info("Job {} fire {} already handled by another instance", job.jobName(), metadata.scheduledFireTime());
    }
}
