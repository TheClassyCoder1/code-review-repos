package com.example.lending.ops.part6.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Publishes loan-funnel stage transitions (applied, KYC passed, offer accepted, disbursed). */
@Component
@RequiredArgsConstructor
public class LoanFunnelAnalyticsPublisher {

    public enum Stage { APPLIED, KYC_PASSED, OFFER_ACCEPTED, DISBURSED, ABANDONED }

    private final FunnelAnalyticsSink sink;

    public void stageReached(Long customerId, Long loanId, Stage stage, String channel) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("event", "loan_funnel_stage");
        event.put("customerId", customerId);
        event.put("loanId", loanId);
        event.put("stage", stage.name());
        event.put("channel", channel == null ? "UNKNOWN" : channel);
        event.put("occurredAt", Instant.now().toString());
        sink.send(event);
    }

    public void agentAssisted(Long customerId, Long applicationId, Long agentId) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("event", "agent_assisted_application");
        event.put("customerId", customerId);
        event.put("applicationId", applicationId);
        event.put("agentId", agentId);
        event.put("occurredAt", Instant.now().toString());
        sink.send(event);
    }
}
