package com.example.lending.loan.auditexport;

import com.example.lending.loan.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AuditExportSessionFactory {

    /** Checks the export token against the shared export key. */
    record ExportTokenValidator(String token, byte[] key) {
        static ExportTokenValidator create(String token, Path keyFile) {
            try {
                return new ExportTokenValidator(token, Files.readAllBytes(keyFile));
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Audit export is not configured");
            }
        }

        boolean validate() {
            return token != null && MessageDigest.isEqual(key, token.getBytes(StandardCharsets.UTF_8));
        }
    }

    private final AuditLogRepository auditLogRepository;
    private final boolean exportEnabled;
    private final int maximumConcurrentExports;
    private final Path signingKeyFile;
    private final AtomicInteger concurrentExports = new AtomicInteger();

    public AuditExportSessionFactory(AuditLogRepository auditLogRepository,
                                     @Value("${audit.export.enabled:false}") boolean exportEnabled,
                                     @Value("${audit.export.max-concurrent:2}") int maximumConcurrentExports,
                                     @Value("${audit.export.signing-key-file}") Path signingKeyFile) {
        this.auditLogRepository = auditLogRepository;
        this.exportEnabled = exportEnabled;
        this.maximumConcurrentExports = maximumConcurrentExports;
        this.signingKeyFile = signingKeyFile;
    }

    public AuditExportSession openSession(String exportToken) {
        if (exportEnabled) {
            if (concurrentExports.get() >= maximumConcurrentExports) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Number of allowed concurrent audit exports exceeded");
            }
            ExportTokenValidator tokenValidator = ExportTokenValidator.create(exportToken, signingKeyFile);
            if (tokenValidator.validate()) {
                return new AuditExportSession(auditLogRepository, concurrentExports);
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No valid token found for audit export");
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit export not enabled");
    }
}
