package com.example.lending.loan.servicing.recon.metrics;

import com.example.lending.loan.servicing.common.ServicingException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Validates metric history queries against the catalog before they reach the metric store. */
@Component
public class MetricQueryValidator {

    private static final Pattern SEGMENT = Pattern.compile("^[a-z][a-z0-9_]{0,40}$");
    private static final Pattern HISTORY = Pattern.compile("^[1-9][0-9]{0,2} (minutes|hours|days)$");

    private static final Map<String, Set<String>> FIELDS = Map.of(
            "recon_matcher_throughput", Set.of("matched", "unmatched", "excluded"),
            "recon_matcher_latency", Set.of("p50_ms", "p95_ms", "p99_ms"),
            "recon_feed_lag", Set.of("lag_seconds", "pending_files"));

    public void validate(String instance, String app, String metrics, String metric, String history) {
        if (!SEGMENT.matcher(instance).matches() || !SEGMENT.matcher(app).matches() || !SEGMENT.matcher(metrics).matches()) {
            throw ServicingException.badRequest("Unknown metric source");
        }
        if (!FIELDS.getOrDefault(app + "_" + metrics, Set.of()).contains(metric)) {
            throw ServicingException.badRequest("Unknown metric field");
        }
        if (!HISTORY.matcher(history).matches()) {
            throw ServicingException.badRequest("History must look like '6 hours'");
        }
    }
}
