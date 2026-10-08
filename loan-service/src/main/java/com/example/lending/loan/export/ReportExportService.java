package com.example.lending.loan.export;

import com.example.lending.loan.document.DocumentStorage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class ReportExportService {

    private static final Pattern ARCHIVE_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,80}\\.(csv|xlsx|pdf)");

    private final ReportExportRepository exportRepository;
    private final DocumentStorage storage;

    public ReportExportService(ReportExportRepository exportRepository, DocumentStorage storage) {
        this.exportRepository = exportRepository;
        this.storage = storage;
    }

    public List<ReportExportJob> listExports(Long ownerId) {
        return exportRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    public ReportExportJob getExport(Long exportId, Long ownerId) {
        return exportRepository.findByIdAndOwnerId(exportId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public InputStream openArchive(Long exportId, Long ownerId, String name) {
        ReportExportJob job = getExport(exportId, ownerId);
        if (name == null || !ARCHIVE_NAME.matcher(name).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid archive name");
        }
        String archiveDir = storage.getRoot().resolve("archive").resolve(String.valueOf(job.getId())).toString();
        InputStream in = ExportArchiveReader.getDownInputStream(name, archiveDir);
        if (in == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return in;
    }

    @Transactional
    public ReportExportJob copyExport(Long exportId, Long targetOwnerId, Map<String, Object> properties) {
        ReportExportJob source = exportRepository.findById(exportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ReportExportJob copy = new ReportExportJob();
        copy.setOwnerId(targetOwnerId);
        copy.setReportType(source.getReportType());
        copy.setStatus(source.getStatus());
        copy.setFileName(source.getFileName());
        copy.setCreatedAt(Instant.now());
        return exportRepository.save(copy);
    }
}
