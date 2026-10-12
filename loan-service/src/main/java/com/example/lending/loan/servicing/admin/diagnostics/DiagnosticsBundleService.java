package com.example.lending.loan.servicing.admin.diagnostics;

import com.example.lending.loan.servicing.admin.diagnostics.StatementRunLogCollector.StatementRunLogSettings;
import com.example.lending.loan.servicing.common.ServicingException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Builds zipped diagnostics bundles with statement render logs for support tickets. */
@Service
public class DiagnosticsBundleService {

    private static final Pattern INSTANCE_NAME = Pattern.compile("^[a-z0-9][a-z0-9-]{0,62}$");
    private static final int MAX_INSTANCES = 50;

    private final StatementRunLogCollector logCollector;
    private final StatementRunLogSettings settings;

    public DiagnosticsBundleService(StatementRunLogCollector logCollector, StatementRunLogSettings settings) {
        this.logCollector = logCollector;
        this.settings = settings;
    }

    public void writeBundle(List<String> requestedInstances, OutputStream out) throws IOException {
        if (requestedInstances.size() > MAX_INSTANCES) {
            throw ServicingException.badRequest("At most " + MAX_INSTANCES + " instances per bundle");
        }
        List<String> instances = new ArrayList<>();
        for (String instance : requestedInstances) {
            if (instance == null || !INSTANCE_NAME.matcher(instance).matches()) {
                throw ServicingException.badRequest("Invalid instance name");
            }
            instances.add(instance);
        }
        Path exportDir = Path.of(settings.bundleDirectory(), UUID.randomUUID().toString());
        Files.createDirectories(exportDir);
        try {
            logCollector.extractLogFromWorkingDir(exportDir.toFile(), instances);
            zip(exportDir, out);
        } finally {
            deleteRecursively(exportDir);
        }
    }

    private static void zip(Path directory, OutputStream out) throws IOException {
        ZipOutputStream zip = new ZipOutputStream(out);
        try (Stream<Path> files = Files.walk(directory)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                zip.putNextEntry(new ZipEntry(directory.relativize(file).toString().replace(File.separatorChar, '/')));
                Files.copy(file, zip);
                zip.closeEntry();
            }
        }
        zip.finish();
    }

    private static void deleteRecursively(Path directory) throws IOException {
        try (Stream<Path> files = Files.walk(directory)) {
            for (Path path : files.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
