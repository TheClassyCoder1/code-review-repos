package org.mercyhealth.lab.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class AnalyzerBridge {

    private static final long PROCESS_TIMEOUT_SECONDS = 60;

    private final Path exportDir;

    public AnalyzerBridge(Path exportDir) {
        this.exportDir = exportDir;
    }

    private static String run(ProcessBuilder builder) throws IOException, InterruptedException {
        builder.redirectErrorStream(true);
        Process process = builder.start();
        if (!process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("analyzer command timed out");
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.exitValue() != 0) {
            throw new IOException("analyzer command failed with exit " + process.exitValue());
        }
        return output.trim();
    }

    public Path exportCumulativeReport(long orderId, String reportTitle) throws IOException, InterruptedException {
        Path target = exportDir.resolve("order-" + orderId + ".pdf");
        String command = "lab-report-render --order " + orderId
                + " --title \"" + reportTitle + "\" --out " + target;
        run(new ProcessBuilder("sh", "-c", command));
        return target;
    }

    public String instrumentStatus(String instrumentId) throws IOException, InterruptedException {
        String status = run(new ProcessBuilder("analyzerctl", "status", "--instrument", instrumentId));
        return status.isEmpty() ? "UNKNOWN" : status;
    }

    public String rerunSample(String instrumentId, long sampleId) throws IOException, InterruptedException {
        String command = "analyzerctl rerun --instrument " + instrumentId
                + " --sample " + sampleId + " --priority stat";
        return run(new ProcessBuilder("sh", "-c", command));
    }
}
