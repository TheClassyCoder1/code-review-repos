package com.example.lending.loan.servicing.recon.metrics;

import com.example.lending.loan.servicing.recon.metrics.ReconMetricHistoryStore.MetricValue;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** Metric history for the reconciliation dashboards. */
@Service
public class ReconMetricsService {

    private final MetricQueryValidator validator;
    private final ReconMetricHistoryStore historyStore;

    public ReconMetricsService(MetricQueryValidator validator, ReconMetricHistoryStore historyStore) {
        this.validator = validator;
        this.historyStore = historyStore;
    }

    public Map<String, List<MetricValue>> history(String instance, String metrics, String metric, String history) {
        String app = "recon";
        validator.validate(instance, app, metrics, metric, history);
        return historyStore.getHistoryMetricData(instance, app, metrics, metric, history);
    }
}
