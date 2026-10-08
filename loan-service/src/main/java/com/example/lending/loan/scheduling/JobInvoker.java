package com.example.lending.loan.scheduling;

import com.example.lending.loan.scheduling.Jobs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/** Runs the registered task behind a job and records how long it took. */
@Component
public class JobInvoker {

    private static final Logger log = LoggerFactory.getLogger(JobInvoker.class);

    private final List<ScheduledTask> tasks;

    public JobInvoker(List<ScheduledTask> tasks) {
        this.tasks = tasks;
    }

    public void init(ScheduledJob job, JobTrigger trigger, ExecutionMetadata metadata) {
        ScheduledTask task = tasks.stream().filter(t -> t.name().equals(job.taskName())).findFirst().orElse(null);
        if (task == null) {
            log.warn("Job {} refers to unknown task {}", job.jobName(), job.taskName());
            return;
        }
        long started = System.nanoTime();
        try {
            task.run();
        } finally {
            log.info("Job {} fire {} took {} ms", job.jobName(), metadata.scheduledFireTime(),
                    (System.nanoTime() - started) / 1_000_000);
        }
    }
}
