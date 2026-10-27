package com.example.lending.loan.batch;

import org.springframework.stereotype.Service;
import reactor.core.publisher.FluxSink;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class TaskLogService {

    private final Map<Long, List<FluxSink<String>>> sinks = new ConcurrentHashMap<>();

    public void registerSink(Long taskId, FluxSink<String> sink) {
        List<FluxSink<String>> taskSinks = sinks.computeIfAbsent(taskId, id -> new CopyOnWriteArrayList<>());
        taskSinks.add(sink);
        sink.onDispose(() -> taskSinks.remove(sink));
    }

    public void append(Long taskId, String line) {
        sinks.getOrDefault(taskId, List.of()).forEach(sink -> sink.next(line));
    }

    public void complete(Long taskId) {
        List<FluxSink<String>> taskSinks = sinks.remove(taskId);
        if (taskSinks != null) {
            taskSinks.forEach(FluxSink::complete);
        }
    }
}
