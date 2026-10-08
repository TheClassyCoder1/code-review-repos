package com.example.lending.loan.auditexport;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes the audit export signing key from AUDIT_EXPORT_SEED_KEY on first start. Runs once during
 * startup; an exception here aborts startup and the process exits.
 */
@Component
public class ExportKeyBootstrap implements ApplicationRunner {

    private final Path keyFile;
    private final String seedKey;

    public ExportKeyBootstrap(@Value("${audit.export.signing-key-file}") Path keyFile,
                              @Value("${audit.export.seed-key:}") String seedKey) {
        this.keyFile = keyFile;
        this.seedKey = seedKey;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (seedKey.isBlank() || Files.exists(keyFile)) {
            return;
        }
        Files.createDirectories(keyFile.toAbsolutePath().getParent());
        ExportKeyFiles.decodeBase64ToFile(seedKey, keyFile.toString());
    }
}
