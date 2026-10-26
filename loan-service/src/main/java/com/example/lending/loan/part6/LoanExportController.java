package com.example.lending.loan.part6;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CSV download of a borrower's loans.
 */
@RestController
@RequestMapping("/api/v1/loan-exports")
public class LoanExportController {

    private final LoanExportService exportService;

    public LoanExportController(LoanExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping(value = "/{userId}", produces = "text/csv")
    public ResponseEntity<String> export(@PathVariable Long userId) {
        String csv = exportService.exportForBorrower(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"loans-" + userId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
