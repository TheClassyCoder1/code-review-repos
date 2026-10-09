package com.example.lending.loan.servicing.admin.diagnostics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

/** Copies render instance logs from the shared working directory into a diagnostics bundle. */
@Component
@EnableConfigurationProperties(StatementRunLogCollector.StatementRunLogSettings.class)
public class StatementRunLogCollector {

    /** Where statement render instances write their logs and how long they are kept. */
    @ConfigurationProperties(prefix = "servicing.diagnostics")
    public record StatementRunLogSettings(String workingDirectory, Duration instanceLogRetention, String bundleDirectory) {

        public StatementRunLogSettings {
            instanceLogRetention = instanceLogRetention == null ? Duration.ofDays(3) : instanceLogRetention;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(StatementRunLogCollector.class);

    private final StatementRunLogSettings config;

    public StatementRunLogCollector(StatementRunLogSettings config) {
        this.config = config;
    }

    public void extractLogFromWorkingDir(File exportDir, List<String> instances) {
        File destLogDir = new File(exportDir, "logs");
        try {
            Files.createDirectories(destLogDir.toPath());
            Path remotePath = Path.of(config.workingDirectory(), "_logs");
            long retentionTime = config.instanceLogRetention().toMillis();
            if (instances.isEmpty()) {
                File[] fileStatuses = remotePath.toFile().listFiles(File::isDirectory);
                for (File fileStatus : fileStatuses == null ? new File[0] : fileStatuses) {
                    if (System.currentTimeMillis() - fileStatus.lastModified() < retentionTime) {
                        instances.add(fileStatus.getName());
                    }
                }
            }
            for (String instance : instances) {
                File toFile = new File(destLogDir, instance);
                Files.createDirectories(toFile.toPath());
                copyDirectoryWithoutError(remotePath.resolve(instance), toFile.toPath());
            }
        } catch (IOException e) {
            logger.error("Failed to extract statement run logs.", e);
        }
    }

    private static void copyDirectoryWithoutError(Path source, Path target) {
        if (!Files.isDirectory(source)) {
            return;
        }
        try (Stream<Path> files = Files.list(source)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                Files.copy(file, target.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            logger.warn("Could not copy logs of {}: {}", source.getFileName(), e.toString());
        }
    }
}
