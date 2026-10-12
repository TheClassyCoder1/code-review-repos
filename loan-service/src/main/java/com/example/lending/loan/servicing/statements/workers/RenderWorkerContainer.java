package com.example.lending.loan.servicing.statements.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Starts and stops statement render workers on the render cluster through the render-submit CLI. */
@Component
@EnableConfigurationProperties(RenderWorkerContainer.RenderWorkerProperties.class)
public class RenderWorkerContainer {

    /** Settings of the statement render cluster. */
    @ConfigurationProperties(prefix = "servicing.statements.workers")
    public record RenderWorkerProperties(String renderHome, String renderMaster, Map<String, String> renderConfig,
                                         Map<String, String> containerProperties) {

        public RenderWorkerProperties {
            renderConfig = renderConfig == null ? Map.of() : Map.copyOf(renderConfig);
            containerProperties = containerProperties == null ? Map.of() : Map.copyOf(containerProperties);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(RenderWorkerContainer.class);

    static final String KUBERNETES_SUBMISSION_ID_PROPERTY = "render.kubernetes.submission.id";

    private final RenderWorkerProperties properties;
    private final String renderHome;
    private final String renderMaster;

    public RenderWorkerContainer(RenderWorkerProperties properties) {
        this.properties = properties;
        this.renderHome = properties.renderHome();
        this.renderMaster = properties.renderMaster();
    }

    public boolean releaseResource(WorkerResource resource) throws IOException, InterruptedException {
        String command = buildReleaseKubernetesCommand(resource);
        Process process = new ProcessBuilder("/bin/sh", "-c", command)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start();
        if (!process.waitFor(2, TimeUnit.MINUTES)) {
            process.destroyForcibly();
            log.warn("Releasing render worker {} timed out", resource.getResourceId());
            return false;
        }
        return process.exitValue() == 0;
    }

    private String buildReleaseKubernetesCommand(WorkerResource resource) {
        Map<String, String> renderConfig = loadRenderConfig();
        if (!resource.getProperties().containsKey(KUBERNETES_SUBMISSION_ID_PROPERTY)) {
            throw new IllegalArgumentException(String.format(
                    "Cannot find %s from worker start up stats.", KUBERNETES_SUBMISSION_ID_PROPERTY));
        }
        RenderConf resourceRenderConf =
                RenderConf.buildFor(renderConfig, getContainerProperties())
                        .withGroupProperties(resource.getProperties())
                        .build();
        String renderOptions = resourceRenderConf.toConfOptions();
        String submissionId = resource.getProperties().get(KUBERNETES_SUBMISSION_ID_PROPERTY);
        return String.format(
                "%s/bin/render-submit --kill %s --master %s %s",
                renderHome, submissionId, renderMaster, renderOptions);
    }

    private Map<String, String> loadRenderConfig() {
        return properties.renderConfig();
    }

    private Map<String, String> getContainerProperties() {
        return properties.containerProperties();
    }
}
