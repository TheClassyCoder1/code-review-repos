package com.example.lending.loan.ops.scoring;

import com.example.lending.loan.support.shell.ShellExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ScoringEngineVersion {

    private static final Logger LOG = LoggerFactory.getLogger(ScoringEngineVersion.class);

    private static final Pattern ENGINE_VERSION_PATTERN = Pattern.compile("engine version\\s+(\\d+\\.\\d+\\.\\d+)");
    private static final Pattern ENGINE_MODEL_VERSION_PATTERN = Pattern.compile("model format\\s+(\\d+\\.\\d+)");

    private final String engineHome;
    private final String version;
    private final String modelFormat;

    public ScoringEngineVersion(String engineHome) {
        this.engineHome = engineHome;
        String[] versions = parseVersion(engineHome);
        this.version = versions[0];
        this.modelFormat = versions[1];
    }

    public String getEngineHome() { return engineHome; }
    public String getVersion() { return version; }
    public String getModelFormat() { return modelFormat; }

    private static String[] parseVersion(String engineHome) {
        final String[] engineVersion = {null, null};
        List<String> cmd =
            Arrays.asList("export SCORING_HOME=" + engineHome + "&&" + engineHome + "/bin/scoring-engine --version");
        StringBuilder buffer = new StringBuilder();
        try {
            ShellExecutor.execute(engineHome, cmd, out -> {
                buffer.append(out).append("\n");
                Matcher m = ENGINE_VERSION_PATTERN.matcher(out);
                if (m.find())
                    engineVersion[0] = m.group(1);
                Matcher m1 = ENGINE_MODEL_VERSION_PATTERN.matcher(out);
                if (m1.find())
                    engineVersion[1] = m1.group(1);
            });
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse scoring engine version from " + engineHome, e);
        }
        LOG.info("[ScoringEngine] {}", buffer);
        if (engineVersion[0] == null || engineVersion[1] == null) {
            throw new IllegalStateException("[ScoringEngine] parse scoring engine version failed. " + buffer);
        }
        return engineVersion;
    }
}
