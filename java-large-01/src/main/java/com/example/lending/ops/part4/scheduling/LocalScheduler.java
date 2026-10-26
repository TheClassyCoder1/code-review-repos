package com.example.lending.ops.part4.scheduling;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Single-node fixed-rate scheduler for console housekeeping jobs. */
@Slf4j
public class LocalScheduler implements Scheduler {

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private volatile boolean paused;

    @Override
    public void scheduleJob(JobDetail jobDetail, Duration interval, Map<String, Object> triggerData) {
        JobDataMap merged = new JobDataMap();
        merged.putAll(jobDetail.jobData());
        merged.putAll(triggerData);
        executor.scheduleAtFixedRate(() -> {
            try {
                if (!paused) {
                    jobDetail.factory().get().execute(() -> merged);
                }
            } catch (JobExecutionException | RuntimeException e) {
                log.error("job {} failed", jobDetail.name(), e);
            }
        }, interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void pauseAll() { paused = true; }
}
