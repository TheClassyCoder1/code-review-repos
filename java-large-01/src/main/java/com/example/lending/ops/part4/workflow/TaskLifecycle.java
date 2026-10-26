package com.example.lending.ops.part4.workflow;

import lombok.Builder;
import org.slf4j.MDC;

import java.util.Date;

/** Types shared by the collections-workflow task lifecycle (dunning letters, field-visit tasks). */
public final class TaskLifecycle {

    public record TaskExecutorFailedLifecycleEvent(Long getWorkflowInstanceId, Long getTaskInstanceId, long getEndTime) { }

    public interface WorkflowEventBus { void publish(Object event); }

    public interface ITaskExecution { WorkflowEventBus getWorkflowEventBus(); }

    @Builder
    public record TaskFailedLifecycleEvent(ITaskExecution taskExecution, Date endTime) { }

    public interface TaskExecutorEventListener {
        void onTaskExecutorFailed(TaskExecutorFailedLifecycleEvent event);
    }

    public static final class LogUtils {
        public static void setWorkflowInstanceIdMDC(Long id) { MDC.put("workflowInstanceId", String.valueOf(id)); }

        public static void removeWorkflowInstanceIdMDC() { MDC.remove("workflowInstanceId"); }
    }
}
