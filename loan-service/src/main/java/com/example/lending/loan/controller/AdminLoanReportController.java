package com.example.lending.loan.controller;

import com.example.lending.loan.dto.LoanExportRequest;
import com.example.lending.loan.service.AdminLoanReportService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/loans")
public class AdminLoanReportController {

    private final AdminLoanReportService reportService;

    public AdminLoanReportController(AdminLoanReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/search")
    public List<Map<String, Object>> search(@RequestParam String tier) {
        return reportService.searchByTier(tier);
    }

    @PostMapping("/export")
    public void export(@RequestBody LoanExportRequest request) {
        reportService.exportToWebhook(request.getTier(), request.getCallbackUrl());
    }
}
