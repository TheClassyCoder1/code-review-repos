package com.example.lending.loan.document.export;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ops/exports")
public class LoanExportController {

    @GetMapping(value = "/loans", produces = "application/json")
    public LoanExportParams loans(@RequestParam(required = false) String status,
                                  @RequestParam(defaultValue = "false") boolean download) {
        LoanExportParams params = new LoanExportParams();
        params.setStatus(status);
        params.setDownload(download);
        return params;
    }
}
