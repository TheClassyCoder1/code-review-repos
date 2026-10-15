package org.mercyhealth.lab;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class LabConfig {

    private final String jdbcUrl;
    private final String jdbcUser;
    private final String jdbcPassword;
    private final Path attachmentRoot;
    private final Path analyzerExportDir;
    private final URI referenceLabBase;
    private final String referenceLabApiKey;
    private final String partnerApiSecret;
    private final Set<String> partnerHosts;
    private final URI portalBase;
    private final Duration httpTimeout;

    private LabConfig(java.util.function.Function<String, String> env) {
        this.jdbcUrl = require(env, "LAB_JDBC_URL");
        this.jdbcUser = require(env, "LAB_JDBC_USER");
        this.jdbcPassword = require(env, "LAB_JDBC_PASSWORD");
        this.attachmentRoot = Path.of(require(env, "LAB_ATTACHMENT_ROOT"));
        this.analyzerExportDir = Path.of(require(env, "LAB_ANALYZER_EXPORT_DIR"));
        this.referenceLabBase = URI.create(require(env, "REFERENCE_LAB_BASE_URL"));
        this.referenceLabApiKey = require(env, "REFERENCE_LAB_API_KEY");
        this.partnerApiSecret = require(env, "PARTNER_API_SECRET");
        this.partnerHosts = Arrays.stream(require(env, "PARTNER_LAB_HOSTS").split(","))
                .map(String::trim)
                .filter(h -> !h.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        this.portalBase = URI.create(require(env, "PORTAL_BASE_URL"));
        this.httpTimeout = Duration.ofSeconds(Long.parseLong(
                env.apply("LAB_HTTP_TIMEOUT_SECONDS") == null ? "10" : env.apply("LAB_HTTP_TIMEOUT_SECONDS")));
    }

    public static LabConfig fromEnvironment() {
        return new LabConfig(System::getenv);
    }

    private static String require(java.util.function.Function<String, String> env, String key) {
        String value = env.apply(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("missing configuration: " + key);
        }
        return value;
    }

    public String jdbcUrl() { return jdbcUrl; }
    public String jdbcUser() { return jdbcUser; }
    public String jdbcPassword() { return jdbcPassword; }
    public Path attachmentRoot() { return attachmentRoot; }
    public Path analyzerExportDir() { return analyzerExportDir; }
    public URI referenceLabBase() { return referenceLabBase; }
    public String referenceLabApiKey() { return referenceLabApiKey; }
    public String partnerApiSecret() { return partnerApiSecret; }
    public Set<String> partnerHosts() { return partnerHosts; }
    public URI portalBase() { return portalBase; }
    public Duration httpTimeout() { return httpTimeout; }
}
