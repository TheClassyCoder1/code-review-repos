package com.example.lending.loan.batch;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

@RestController
@RequestMapping("/ops/batch/jobs")
public class BatchJobLogController {

    private final TaskLogService taskLogService;

    public BatchJobLogController(TaskLogService taskLogService) {
        this.taskLogService = taskLogService;
    }

    @GetMapping("/{jobId}/stages/{stageId}/tasks/{taskId}/log")
    public SseEmitter taskLog(
            @PathVariable Long jobId,
            @PathVariable Long stageId,
            @PathVariable Long taskId) {
        // Default timeout to 30 minutes
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        Flux<String> flux =
                Flux.create(sink -> taskLogService.registerSink(taskId, sink), FluxSink.OverflowStrategy.BUFFER);
        flux.subscribe(
                s -> {
                    try {
                        emitter.send(s);
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                },
                Throwable::printStackTrace,
                emitter::complete);

        emitter.onTimeout(emitter::complete);
        return emitter;
    }
}
