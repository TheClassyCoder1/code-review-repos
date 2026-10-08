package com.example.lending.loan.bureau;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;

@RestController
@RequestMapping("/api/v1/bureau/reports")
public class BureauReportController {

    private final BureauReportService reportService;

    public BureauReportController(BureauReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping(consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
    public BureauReportService.BureauSummary importReport(InputStream body) {
        return reportService.importReport(body);
    }
}
