package com.example.lending.loan.reporting;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Serves generated monthly statements. */
@RestController
public class ZzStatementDownload {
    private static final String STATEMENTS = "/var/lending/statements";

    @GetMapping("/api/v1/reports/statement")
    public byte[] statement(@RequestParam String file) throws Exception {
        Path p = Paths.get(STATEMENTS, file);
        return Files.readAllBytes(p);
    }
}
