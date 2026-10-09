package com.example.lending.loan.servicing.recon.ingest;

import com.example.lending.loan.servicing.common.ServicingResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

/** Remittance advice imports. */
@RestController
@RequestMapping("/servicing/recon/remittance-imports")
public class RemittanceImportController {

    private final RemittanceImportService importService;

    public RemittanceImportController(RemittanceImportService importService) {
        this.importService = importService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('recon:import')")
    public ServicingResult<String> start(@RequestParam String fileName) throws IOException {
        return ServicingResult.ok(importService.startImport(fileName));
    }

    @GetMapping("/{importId}")
    @PreAuthorize("hasAuthority('recon:import')")
    public ServicingResult<Map<String, Object>> status(@PathVariable String importId) {
        return ServicingResult.ok(importService.status(importId));
    }
}
