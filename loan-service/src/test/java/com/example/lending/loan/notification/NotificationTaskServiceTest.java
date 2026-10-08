package com.example.lending.loan.notification;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

class NotificationTaskServiceTest {

    private final EventRecorder eventRecorder = new EventRecorder();
    private final Map<String, TaskDefinition> tasks = new ConcurrentHashMap<>();
    private final NotificationTaskService taskService = new NotificationTaskService(
            definition -> tasks.putIfAbsent(definition.getIdentifier(), definition) == null, eventRecorder);

    @Test
    void shouldNotCreateTaskAlreadyExists() throws Exception {
        final TaskDefinition taskDefinition = TaskGenerator.createRandomTask(TaskDefinition.Type.CUSTOM, Boolean.FALSE, Boolean.FALSE);
        this.taskService.createTask(taskDefinition);

        this.eventRecorder.wait(NotificationTaskService.POST_TASK, taskDefinition.getIdentifier());

        try {
            this.taskService.createTask(taskDefinition);
            Assertions.fail();
        } catch (final NotificationTaskService.TaskAlreadyExistsException ex) {
            // do nothing, expected
        }
    }

    static final class TaskGenerator {
        static TaskDefinition createRandomTask(TaskDefinition.Type type, Boolean mandatory, Boolean predefined) {
            TaskDefinition definition = new TaskDefinition();
            definition.setIdentifier(UUID.randomUUID().toString().substring(0, 8));
            definition.setType(type);
            definition.setName("task-" + definition.getIdentifier());
            definition.setMandatory(mandatory);
            definition.setPredefined(predefined);
            return definition;
        }
    }

    static final class EventRecorder implements ApplicationEventPublisher {
        private final Set<Object> events = ConcurrentHashMap.newKeySet();

        @Override
        public void publishEvent(Object event) {
            events.add(event);
        }

        void wait(String event, String identifier) {
            Assertions.assertTrue(events.contains(new NotificationTaskService.TaskEvent(event, identifier)));
        }
    }
}
