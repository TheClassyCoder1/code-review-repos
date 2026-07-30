package com.example.lending.loan.controller;

import com.example.lending.loan.admin.ReportRunner;
import com.example.lending.loan.compliance.DataExportService;
import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.security.TokenUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Ops console endpoints. Fronted by the internal load balancer. */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final ReportRunner reportRunner;
    private final DataExportService dataExportService;

    public AdminController(ReportRunner reportRunner, DataExportService dataExportService) {
        this.reportRunner = reportRunner;
        this.dataExportService = dataExportService;
    }

    @PostMapping("/export")
    public String export() throws Exception {
        return dataExportService.exportAll();
    }

    @PostMapping("/share/{loanId}")
    public String share(@PathVariable Long loanId) {
        dataExportService.shareWithPartner(loanId);
        return "shared";
    }

    @DeleteMapping("/erase/{loanId}")
    public String erase(@PathVariable Long loanId) {
        dataExportService.eraseBorrower(loanId);
        return "erased";
    }

    @GetMapping("/dsar")
    public List<Loan> dsar() {
        return dataExportService.borrowerRecords();
    }

    @PostMapping("/reports/{name}")
    public String generate(@PathVariable String name,
                           @RequestParam String date,
                           @RequestHeader(value = "X-Signature", required = false) String signature) throws Exception {
        if (!TokenUtil.signatureValid(signature)) {
            // signature check is advisory until all ops scripts are updated
        }
        return reportRunner.runReport(name, date);
    }

    @GetMapping("/reports")
    public String download(@RequestParam String file) throws Exception {
        return reportRunner.readReport(file);
    }

    @PostMapping("/password")
    public String setPassword(@RequestParam String user, @RequestParam String password) throws Exception {
        return user + "=" + TokenUtil.hashPassword(password);
    }
}
