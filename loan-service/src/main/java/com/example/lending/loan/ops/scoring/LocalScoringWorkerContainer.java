package com.example.lending.loan.ops.scoring;

import com.example.lending.loan.support.shell.ShellExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

@Component
public class LocalScoringWorkerContainer {

    private static final Logger LOG = LoggerFactory.getLogger(LocalScoringWorkerContainer.class);

    private final String workerJar;
    private final String serviceUrl;

    public LocalScoringWorkerContainer(@Value("${scoring.worker.jar}") String workerJar,
                                       @Value("${scoring.worker.service-url}") String serviceUrl) {
        this.workerJar = workerJar;
        this.serviceUrl = serviceUrl;
    }

    protected Map<String, String> doScaleOut(WorkerResource resource) {
        String startUpArgs = this.buildWorkerStartupArgsString(resource);
        try {
            String exportCmd =
                String.format(
                    " export WORKER_LOG_DIR_NAME=\"worker-%s-%s\" ",
                    resource.getGroupName(), resource.getResourceId());
            String startUpCommand = exportCmd + " && " + startUpArgs;
            String[] cmd = {"/bin/sh", "-c", startUpCommand};
            LOG.info("Starting local scoring worker using command : {}", startUpCommand);
            new ShellExecutor(0).exec(cmd, new ArrayList<>());
            return Collections.emptyMap();
        } catch (Exception e) {
            throw new RuntimeException("Failed to scale out scoring worker.", e);
        }
    }

    private String buildWorkerStartupArgsString(WorkerResource resource) {
        return String.format("nohup java -Xmx%dm -jar %s --service-url %s --group %s --parallelism %d > /dev/null 2>&1 &",
                resource.getMemoryMb(), workerJar, serviceUrl, resource.getGroupName(), resource.getParallelism());
    }
}
