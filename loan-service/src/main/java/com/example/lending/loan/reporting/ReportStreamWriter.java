package com.example.lending.loan.reporting;

import org.slf4j.*;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;

@Component
public class ReportStreamWriter {

    public record ReportChunk(List<Map<String, Object>> rows, String message) {
    }

    private static final Logger log = LoggerFactory.getLogger(ReportStreamWriter.class);

    void sendChunk(SseEmitter emitter, List<Map<String, Object>> rows, String message) {
        try {
            emitter.send(SseEmitter.event().name(rows == null ? "status" : "rows").data(new ReportChunk(rows, message)));
        } catch (IOException e) {
            log.debug("Report stream client disconnected");
        }
    }

    void handleError(SseEmitter emitter, Throwable throwable) {
        log.error("Error during report streaming: {}", throwable.getMessage(), throwable);
        sendChunk(emitter, null, "Error: " + throwable.getMessage());
        emitter.completeWithError(throwable);
    }
}
