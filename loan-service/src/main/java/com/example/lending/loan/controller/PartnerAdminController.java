package com.example.lending.loan.controller;

import com.example.lending.loan.dto.PartnerProductDefinition;
import com.example.lending.loan.dto.StatementSyncRequest;
import com.example.lending.loan.partner.PartnerRateSheetClient;
import com.example.lending.loan.service.PartnerProductService;
import com.example.lending.loan.service.SchemaReportService;
import com.example.lending.loan.service.StatementSyncService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/partner")
public class PartnerAdminController {

    private static final String STANDARD_RATE_SHEET = "standard.csv";

    private final PartnerRateSheetClient rateSheetClient;
    private final SchemaReportService schemaReportService;
    private final PartnerProductService partnerProductService;
    private final StatementSyncService statementSyncService;

    public PartnerAdminController(PartnerRateSheetClient rateSheetClient,
                                  SchemaReportService schemaReportService,
                                  PartnerProductService partnerProductService,
                                  StatementSyncService statementSyncService) {
        this.rateSheetClient = rateSheetClient;
        this.schemaReportService = schemaReportService;
        this.partnerProductService = partnerProductService;
        this.statementSyncService = statementSyncService;
    }

    @PostMapping("/rates/refresh")
    public String refreshRates() throws IOException, InterruptedException {
        return rateSheetClient.getRateSheetFile(STANDARD_RATE_SHEET);
    }

    @GetMapping("/schema/primary-keys")
    public Map<String, List<String>> primaryKeys(@RequestParam List<String> tables) throws SQLException {
        return schemaReportService.primaryKeys(tables);
    }

    @PostMapping("/products")
    public ResponseEntity<Void> createProduct(@RequestBody final PartnerProductDefinition definition) {
        if (this.partnerProductService.productDefinitionExists(definition.identifier())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Product definition " + definition.identifier() + " already exists.");
        } else {
            this.partnerProductService.create(definition);
        }
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/statement-sync")
    public String syncStatement(@RequestBody StatementSyncRequest request) throws IOException {
        return statementSyncService.pullFromPeer(request);
    }
}
