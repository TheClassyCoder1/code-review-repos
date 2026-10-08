package com.example.lending.loan.batch;

import com.example.lending.loan.support.web.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.security.Principal;
import java.util.Set;

@RestController
@RequestMapping("/ops/batch/jobs")
public class BatchJobController {

    private final BatchJobService sysJobService;
    private final BatchJobScheduler taskUtil;
    private final TaskScheduler scheduler;
    private final Set<String> allowedTaskHosts;

    public BatchJobController(BatchJobService sysJobService, BatchJobScheduler taskUtil, TaskScheduler scheduler,
                              @Value("${batch.jobs.allowed-hosts}") Set<String> allowedTaskHosts) {
        this.sysJobService = sysJobService;
        this.taskUtil = taskUtil;
        this.scheduler = scheduler;
        this.allowedTaskHosts = allowedTaskHosts;
    }

    @PutMapping
    public ApiResponse<Void> updateById(Principal principal, @RequestBody BatchJob sysJob) {
        if (!isRestTaskUrlAllowed(sysJob)) {
            return ApiResponse.ofFail("Task URL host is not in the allow list");
        }
        sysJob.setUpdateBy(principal.getName());
        BatchJob querySysJob = this.sysJobService.getById(sysJob.getJobId());
        if (BatchJobStatus.JOB_STATUS_NOT_RUNNING.getType().equals(querySysJob.getJobStatus())) {
            this.taskUtil.addOrUpdateJob(sysJob, scheduler);
            sysJobService.updateById(sysJob);
        }
        else if (BatchJobStatus.JOB_STATUS_RELEASE.getType().equals(querySysJob.getJobStatus())) {
            sysJobService.updateById(sysJob);
        }
        return ApiResponse.ofSuccess();
    }

    private boolean isRestTaskUrlAllowed(BatchJob job) {
        try {
            URI uri = URI.create(job.getTargetUrl());
            return "https".equals(uri.getScheme()) && uri.getHost() != null && allowedTaskHosts.contains(uri.getHost());
        } catch (IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }
}
