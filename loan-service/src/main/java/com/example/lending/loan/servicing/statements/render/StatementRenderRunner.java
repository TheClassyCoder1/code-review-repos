package com.example.lending.loan.servicing.statements.render;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Runs a statement renderer jar from the resource store as a child JVM. */
@Service
public class StatementRenderRunner {

    private static final long RENDER_TIMEOUT_MINUTES = 10;

    private final Path resourceDirectory;
    private final Path javaHome;

    public StatementRenderRunner(@Value("${servicing.statements.render.resource-dir}") String resourceDirectory,
                                 @Value("${servicing.statements.render.java-home:${java.home}}") String javaHome) {
        this.resourceDirectory = Path.of(resourceDirectory).toAbsolutePath().normalize();
        this.javaHome = Path.of(javaHome);
    }

    public int render(RenderParameters parameters, Long statementId, String period) throws IOException, InterruptedException {
        Path mainJar = resourceDirectory.resolve(parameters.getMainJar().getResourceName()).normalize();
        if (!mainJar.startsWith(resourceDirectory) || !Files.isRegularFile(mainJar)) {
            throw new IllegalArgumentException("Unknown renderer jar");
        }
        ResourceContext resources = new ResourceContext(Map.of(parameters.getMainJar().getResourceName(),
                new ResourceContext.ResourceItem(parameters.getMainJar().getResourceName(), mainJar)));
        RenderTaskRequest request = new RenderTaskRequest(resources,
                Map.of("statementId", String.valueOf(statementId), "period", period));
        String command = new StatementRenderCommandBuilder(request, parameters, javaHome).buildJarCommand();

        List<String> arguments = Arrays.asList(command.trim().split("\\s+"));
        Process process = new ProcessBuilder(arguments)
                .directory(resourceDirectory.toFile())
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start();
        if (!process.waitFor(RENDER_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
            process.destroyForcibly();
            return -1;
        }
        return process.exitValue();
    }
}
