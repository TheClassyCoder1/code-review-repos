package com.example.lending.ops.part2.optimizer;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Starts and stops optimizer workers on some kind of compute (local processes, Spark on k8s). */
public abstract class AbstractOptimizerContainer {

    private Map<String, String> containerProperties = Map.of();

    /** Container settings come from the ops config file and pass the same check as {@link Resource}. */
    public void init(Map<String, String> properties) {
        Resource.validateProperties(properties);
        containerProperties = Map.copyOf(properties);
    }

    protected Map<String, String> getContainerProperties() {
        return containerProperties;
    }

    protected Map<String, String> doScaleOut(Resource resource) {
        throw new UnsupportedOperationException("this container does not start optimizers");
    }

    protected String buildOptimizerStartupArgsString(Resource resource) {
        return String.format("%s/bin/optimizer.sh start -a %s -g %s --resource-id %s",
                containerProperties.getOrDefault("optimizer-home", "/opt/optimizer"),
                containerProperties.getOrDefault("ams-optimizing-uri", "thrift://127.0.0.1:1261"),
                resource.getGroupName(), resource.getResourceId());
    }

    protected interface ExecUtil {
        static void exec(String[] cmd, List<String> output) throws IOException, InterruptedException {
            Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            process.inputReader().lines().limit(1000).forEach(output::add);
            if (!process.waitFor(120, TimeUnit.SECONDS) || process.exitValue() != 0) {
                process.destroyForcibly();
                throw new IOException("command failed or timed out");
            }
        }
    }
}
