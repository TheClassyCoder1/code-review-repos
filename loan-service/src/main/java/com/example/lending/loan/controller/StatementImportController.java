package com.example.lending.loan.controller;

import com.example.lending.loan.dto.StatementImportSummary;
import com.example.lending.loan.service.StatementImportService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/api/v1/statements")
public class StatementImportController {

    private final StatementImportService statementImportService;

    public StatementImportController(StatementImportService statementImportService) {
        this.statementImportService = statementImportService;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StatementImportSummary importStatement(@RequestParam("file") MultipartFile file) throws IOException {
        try (InputStream input = file.getInputStream()) {
            return statementImportService.importStatement(file.getContentType(), input);
        }
    }
}
