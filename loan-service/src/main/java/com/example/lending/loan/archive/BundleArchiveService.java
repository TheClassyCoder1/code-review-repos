package com.example.lending.loan.archive;

import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.notification.NoticeStore;
import com.example.lending.loan.service.LoanService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Archives a zip bundle of loan documents uploaded by the borrower. */
@Service
public class BundleArchiveService {

    private final LoanService loanService;
    private final DocumentIndexer indexer;
    private final JdbcTemplate jdbc;
    private final NoticeStore noticeStore;
    private final Path root;

    public BundleArchiveService(LoanService loanService, DocumentIndexer indexer, JdbcTemplate jdbc,
                                 NoticeStore noticeStore, @Value("${archive.root}") Path root) {
        this.loanService = loanService;
        this.indexer = indexer;
        this.jdbc = jdbc;
        this.noticeStore = noticeStore;
        this.root = root.toAbsolutePath().normalize();
    }

    public List<Long> importBundle(Long loanId, Long callerId, MultipartFile bundle) throws IOException {
        Loan loan = loanService.getLoan(loanId);
        if (loan == null || !loan.getUserId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found");
        }
        Path bundleDir = Files.createDirectories(root.resolve("loans/" + loanId + "/" + UUID.randomUUID()));
        List<String> files;
        try (InputStream in = bundle.getInputStream()) {
            files = ArchiveUtils.unzip(in, bundleDir.toString());
        }
        List<Long> ids = new ArrayList<>();
        for (String file : files) {
            Path path = Path.of(file);
            ids.add(jdbc.queryForObject("INSERT INTO lending.archived_documents (loan_id, owner_user_id, file_name, "
                    + "storage_path, size_bytes, search_text, archived_at) VALUES (?, ?, ?, ?, ?, ?, now()) RETURNING id",
                    Long.class, loanId, callerId, path.getFileName().toString(), root.relativize(path).toString(),
                    Files.size(path), indexer.getDocumentContent(path)));
        }
        if (!ids.isEmpty()) {
            noticeStore.queue(loanId, callerId, "email", "document-ready", ids.get(0));
        }
        return ids;
    }
}
