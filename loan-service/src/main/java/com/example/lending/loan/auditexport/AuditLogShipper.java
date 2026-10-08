package com.example.lending.loan.auditexport;

import com.example.lending.loan.auditexport.AuditLogProducer.LogItem;
import com.example.lending.loan.auditexport.AuditLogProducer.LogSizeTooLargeException;
import com.example.lending.loan.auditexport.AuditLogProducer.MaxBatchCountExceedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;

/** Forwards ops audit records to the central audit log store. */
@Component
public class AuditLogShipper {

    private static final Logger LOG = LoggerFactory.getLogger(AuditLogShipper.class);
    private static final String DEFAULT_SOURCE = "loan-service";

    record OpsRequestLog(String method, String requestUri, Long actorId, int status) {
    }

    private final AuditLogProducer producer;
    private final Executor threadExecutor = ForkJoinPool.commonPool();
    private final String projectName = "lending";
    private final String logStore = "ops-audit";
    private final String topic = "audit.ops";

    public AuditLogShipper(AuditLogProducer producer) {
        this.producer = producer;
    }

    public void ship(String method, String requestUri, Long actorId, int status) {
        sendLog(new OpsRequestLog(method, requestUri, actorId, status));
    }

    private void sendLog(final OpsRequestLog log) {
        final List<LogItem> logGroup = new ArrayList<>();
        LogItem logItem = new LogItem((int) (System.currentTimeMillis() / 1000));
        logItem.pushBack("level", "info");
        logItem.pushBack("name", log.requestUri());
        logItem.pushBack("message", log.toString());
        logGroup.add(logItem);
        try {
            CompletableFuture<Long> f = producer.send(projectName, logStore, topic, DEFAULT_SOURCE, logGroup);
            f.whenCompleteAsync(new ProducerFutureCallback(projectName, logStore), threadExecutor);
        } catch (InterruptedException e) {
            LOG.warn("The current thread has been interrupted during send logs.");
        } catch (Exception e) {
            if (e instanceof MaxBatchCountExceedException) {
                LOG.error("The logs exceeds the maximum batch count, e={}", e.getMessage());
            } else if (e instanceof LogSizeTooLargeException) {
                LOG.error("The size of log is larger than the maximum allowable size, e={}", e.getMessage());
            } else {
                LOG.error("Failed to send logs, e={}", e.getMessage());
            }
        }
    }

    private record ProducerFutureCallback(String project, String logStore) implements BiConsumer<Long, Throwable> {
        @Override
        public void accept(Long offset, Throwable error) {
            if (error != null) {
                LOG.error("Failed to ship audit logs to {}/{}, e={}", project, logStore, error.getMessage());
            }
        }
    }
}
