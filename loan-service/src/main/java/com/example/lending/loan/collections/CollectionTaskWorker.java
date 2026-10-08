package com.example.lending.loan.collections;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
public class CollectionTaskWorker {

    private final CollectionTaskStore store = new CollectionTaskStore();
    private final ExecutorService writer =
            Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "collection-task-writer"));

    public Future<Boolean> open(String taskId, Long loanId, long taskEpoch) {
        return writer.submit(() -> store.create(taskId, loanId, taskEpoch));
    }

    public Future<Boolean> complete(String taskId, long taskEpoch, String status, String output) {
        return writer.submit(() -> store.updateStatus(taskId, taskEpoch, status, output));
    }

    @PreDestroy
    void shutdown() {
        writer.shutdown();
    }
}
