package com.example.lending.loan.servicing.collections.workflow;

import com.example.lending.loan.servicing.collections.dialer.CollectionEvent;
import com.example.lending.loan.servicing.collections.dialer.DialerEventSink;
import com.example.lending.loan.servicing.collections.dialer.CollectionEvent.EventData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Forwards finished call-campaign steps to the dialer so it can stop or schedule calls. */
@Component
public class DialerEventBridge {

    private static final String CALL_STEP_PREFIX = "call-";

    private final DialerEventSink sink;
    private final ObjectMapper objectMapper;

    public DialerEventBridge(DialerEventSink sink, ObjectMapper objectMapper) {
        this.sink = sink;
        this.objectMapper = objectMapper;
    }

    @EventListener
    public void onStepFinished(CaseStepFinishedEvent event) throws JsonProcessingException {
        if (!event.stepKey().startsWith(CALL_STEP_PREFIX)) {
            return;
        }
        byte[] body = objectMapper.writeValueAsBytes(Map.of(
                "caseId", event.caseId(),
                "step", event.stepKey(),
                "state", event.state().name()));
        sink.put(List.of(new CollectionEvent(UUID.randomUUID().toString(), event.caseId(), "STEP_FINISHED",
                Instant.now(), new EventData(body))));
    }
}
