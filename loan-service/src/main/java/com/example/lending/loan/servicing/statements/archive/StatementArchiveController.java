package com.example.lending.loan.servicing.statements.archive;

import com.example.lending.loan.servicing.common.ServicingResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;

/** Upload of rendered statement PDFs by the render workers. */
@RestController
@RequestMapping("/servicing/statements")
public class StatementArchiveController {

    private final StatementArchiveService archiveService;

    public StatementArchiveController(StatementArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @PutMapping(value = "/{statementId}/document", consumes = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('statement:archive')")
    public ServicingResult<String> upload(@PathVariable Long statementId, HttpServletRequest request) throws IOException {
        try (InputStream document = request.getInputStream()) {
            return ServicingResult.ok(archiveService.store(statementId, document));
        }
    }
}
