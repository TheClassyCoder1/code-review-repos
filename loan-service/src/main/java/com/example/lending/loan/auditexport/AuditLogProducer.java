package com.example.lending.loan.auditexport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Ships batches of audit log items to Kafka. */
@Component
public class AuditLogProducer {

    public static class LogItem {
        public final int time;
        public final Map<String, String> contents = new LinkedHashMap<>();

        public LogItem(int time) {
            this.time = time;
        }

        public void pushBack(String key, String value) {
            contents.put(key, value);
        }
    }

    public static class MaxBatchCountExceedException extends Exception {
        MaxBatchCountExceedException(String message) {
            super(message);
        }
    }

    public static class LogSizeTooLargeException extends Exception {
        LogSizeTooLargeException(String message) {
            super(message);
        }
    }

    private static final int MAX_BATCH_COUNT = 512;
    private static final int MAX_BATCH_BYTES = 512 * 1024;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public AuditLogProducer(@Qualifier("brokerATemplate") KafkaTemplate<String, String> kafkaTemplate,
                            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public CompletableFuture<Long> send(String project, String logStore, String topic, String source,
                                        List<LogItem> logGroup) throws Exception {
        if (logGroup.size() > MAX_BATCH_COUNT) {
            throw new MaxBatchCountExceedException("batch count " + logGroup.size());
        }
        String payload = objectMapper.writeValueAsString(
                Map.of("project", project, "logStore", logStore, "source", source, "items", logGroup));
        if (payload.getBytes(StandardCharsets.UTF_8).length > MAX_BATCH_BYTES) {
            throw new LogSizeTooLargeException("batch size " + payload.length());
        }
        return kafkaTemplate.send(topic, logStore, payload).thenApply(result -> result.getRecordMetadata().offset());
    }
}
