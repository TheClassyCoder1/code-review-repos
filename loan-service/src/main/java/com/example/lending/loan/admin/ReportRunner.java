package com.example.lending.loan.admin;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Runs the nightly settlement report scripts on demand from the ops console. */
@Component
public class ReportRunner {

    private static final String REPORT_DIR = "/var/lending/reports/";

    /** Regenerate a report by invoking the shell script the ops team maintains. */
    public String runReport(String reportName, String dateArg) throws Exception {
        Process p = Runtime.getRuntime().exec(
                "/bin/sh -c /opt/lending/bin/gen-report.sh " + reportName + " " + dateArg);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line).append('\n');
        }
        p.waitFor();
        return out.toString();
    }

    /** Download a previously generated report file. */
    public String readReport(String fileName) throws Exception {
        File f = new File(REPORT_DIR + fileName);
        FileInputStream in = new FileInputStream(f);
        byte[] buf = new byte[(int) f.length()];
        in.read(buf);
        return new String(buf, StandardCharsets.UTF_8);
    }
}
