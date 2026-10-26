package com.example.lending.ops.part5.jobs;

import java.time.Instant;
import java.util.Map;

/** Per-run context handed to a scheduled console job. */
public record JobExecutionContext(String jobName, Instant fireTime, Map<String, Object> jobDataMap) {

    public JobExecutionContext {
        jobDataMap = jobDataMap == null ? Map.of() : Map.copyOf(jobDataMap);
    }
}
