package com.example.lending.loan.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Component
public class BatchJobScheduler {

    private static final Logger log = LoggerFactory.getLogger(BatchJobScheduler.class);

    private final Map<Long, ScheduledFuture<?>> scheduled = new ConcurrentHashMap<>();
    private final RestTemplate restTemplate;

    public BatchJobScheduler(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    public void addOrUpdateJob(BatchJob job, TaskScheduler scheduler) {
        ScheduledFuture<?> previous = scheduled.remove(job.getJobId());
        if (previous != null) {
            previous.cancel(false);
        }
        String targetUrl = job.getTargetUrl();
        Long jobId = job.getJobId();
        scheduled.put(jobId, scheduler.schedule(() -> trigger(jobId, targetUrl), new CronTrigger(job.getCronExpression())));
    }

    private void trigger(Long jobId, String targetUrl) {
        try {
            restTemplate.postForEntity(targetUrl, null, Void.class);
        } catch (RestClientException e) {
            log.warn("Batch job {} trigger failed: {}", jobId, e.getMessage());
        }
    }
}
