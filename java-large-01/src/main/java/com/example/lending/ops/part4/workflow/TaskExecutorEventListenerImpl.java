package com.example.lending.ops.part4.workflow;

import com.example.lending.ops.part4.workflow.TaskLifecycle.ITaskExecution;
import com.example.lending.ops.part4.workflow.TaskLifecycle.LogUtils;
import com.example.lending.ops.part4.workflow.TaskLifecycle.TaskExecutorEventListener;
import com.example.lending.ops.part4.workflow.TaskLifecycle.TaskExecutorFailedLifecycleEvent;
import com.example.lending.ops.part4.workflow.TaskLifecycle.TaskFailedLifecycleEvent;

import java.util.Date;
import java.util.Map;

/** Turns executor callbacks into workflow events on the owning workflow's event bus. */
public class TaskExecutorEventListenerImpl implements TaskExecutorEventListener {

    private final Map<Long, ITaskExecution> runningTasks;

    public TaskExecutorEventListenerImpl(Map<Long, ITaskExecution> runningTasks) { this.runningTasks = runningTasks; }

    @Override
    public void onTaskExecutorFailed(final TaskExecutorFailedLifecycleEvent taskExecutorFailedLifecycleEvent) {
        LogUtils.setWorkflowInstanceIdMDC(taskExecutorFailedLifecycleEvent.getWorkflowInstanceId());
        try {
            final ITaskExecution taskExecution =
                    getTaskExecution(taskExecutorFailedLifecycleEvent);
            final TaskFailedLifecycleEvent taskFailedEvent = TaskFailedLifecycleEvent.builder()
                    .taskExecution(taskExecution)
                    .endTime(new Date(taskExecutorFailedLifecycleEvent.getEndTime()))
                    .build();
            taskExecution.getWorkflowEventBus().publish(taskFailedEvent);
        } finally {
            LogUtils.removeWorkflowInstanceIdMDC();
        }
    }

    private ITaskExecution getTaskExecution(final TaskExecutorFailedLifecycleEvent event) {
        ITaskExecution execution = runningTasks.get(event.getTaskInstanceId());
        if (execution == null) {
            throw new IllegalStateException("No running task " + event.getTaskInstanceId());
        }
        return execution;
    }
}
