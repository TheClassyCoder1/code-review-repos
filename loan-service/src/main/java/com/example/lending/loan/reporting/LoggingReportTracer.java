package com.example.lending.loan.reporting;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoggingReportTracer extends ReportRequestTracer {

    private static final Logger log = LoggerFactory.getLogger(LoggingReportTracer.class);

    public LoggingReportTracer(@Value("${reports.tracing.enabled:false}") boolean enabled) {
        setEnabled(enabled);
    }

    @Override
    public void trace(String message, Object context) {
        super.trace(message, context);
        if (!isEnabled()) {
            return;
        }

        logMessage(message, context);
    }

    private void logMessage(String message, Object context) {
        log.info("[report-trace] {} context={}", message, context);
    }
}
