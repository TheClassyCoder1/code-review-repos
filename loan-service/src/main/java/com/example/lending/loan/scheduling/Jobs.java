package com.example.lending.loan.scheduling;

import java.time.Instant;
import java.util.Map;

public final class Jobs {

    private Jobs() {
    }

    public record ScheduledJob(String jobName, String taskName) {
    }

    public record JobTrigger(String name, Instant scheduledFireTime, Instant fireTime) {
    }

    public record ExecutionMetadata(String jobName, Instant scheduledFireTime, Instant fireTime) {
    }

    public enum JobDataKey {
        SCHEDULE_JOB_KEY("scheduleJob");

        private final String type;

        JobDataKey(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    public record JobExecutionContext(Map<String, Object> mergedJobDataMap, JobTrigger trigger) {

        public Map<String, Object> getMergedJobDataMap() {
            return mergedJobDataMap;
        }

        public JobTrigger getTrigger() {
            return trigger;
        }
    }
}
