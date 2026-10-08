package com.example.lending.loan.notification;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class NotificationTaskService {

    public static final String POST_TASK = "post-task";

    public record TaskEvent(String event, String identifier) {
    }

    public interface TaskStore {
        /** Stores the definition; false when one with the same identifier already exists. */
        boolean insertIfAbsent(TaskDefinition definition);
    }

    public static class TaskAlreadyExistsException extends ResponseStatusException {
        public TaskAlreadyExistsException(String identifier) {
            super(HttpStatus.CONFLICT, "Task " + identifier + " already exists");
        }
    }

    @Repository
    static class JdbcTaskStore implements TaskStore {
        private final JdbcTemplate jdbc;

        JdbcTaskStore(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @Override
        public boolean insertIfAbsent(TaskDefinition d) {
            return jdbc.update("INSERT INTO lending.notification_task_definitions (identifier, type, name, mandatory, "
                    + "predefined) VALUES (?, ?, ?, ?, ?) ON CONFLICT (identifier) DO NOTHING", d.getIdentifier(),
                    d.getType() == null ? null : d.getType().name(), d.getName(), d.getMandatory(), d.getPredefined()) == 1;
        }
    }

    private final TaskStore store;
    private final ApplicationEventPublisher eventPublisher;

    public NotificationTaskService(TaskStore store, ApplicationEventPublisher eventPublisher) {
        this.store = store;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/api/v1/ops/notification-tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public void createTask(@RequestBody TaskDefinition definition) {
        if (!store.insertIfAbsent(definition)) {
            throw new TaskAlreadyExistsException(definition.getIdentifier());
        }
        eventPublisher.publishEvent(new TaskEvent(POST_TASK, definition.getIdentifier()));
    }
}
