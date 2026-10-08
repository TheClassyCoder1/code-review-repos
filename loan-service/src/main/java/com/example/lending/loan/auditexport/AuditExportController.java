package com.example.lending.loan.auditexport;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
public class AuditExportController {

    private final AuditExportSessionFactory sessionFactory;

    public AuditExportController(AuditExportSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @GetMapping("/api/v1/ops/audit-export")
    public ResponseEntity<StreamingResponseBody> export(
            @RequestHeader(value = "X-Export-Token", required = false) String exportToken) {
        AuditExportSession session = sessionFactory.openSession(exportToken);
        StreamingResponseBody body = out -> {
            try (session) {
                session.writeCsv(out);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-log.csv\"")
                .contentType(new MediaType("text", "csv"))
                .body(body);
    }
}
