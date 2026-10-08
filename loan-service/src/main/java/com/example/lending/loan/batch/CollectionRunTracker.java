package com.example.lending.loan.batch;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks the execution state of the task slots of one collections dialer run. */
public class CollectionRunTracker {

    public enum ExecutionState { CREATED, SCHEDULED, RUNNING, FINISHED, FAILED }

    public record TaskSlot(long runId, int slot) {
    }

    private final List<TaskSlot> taskGroupLocations;
    private final Map<TaskSlot, Long[]> runningJobStateTimestampsMap = new ConcurrentHashMap<>();
    private final Map<TaskSlot, ExecutionState> runningJobStateMap = new ConcurrentHashMap<>();

    public CollectionRunTracker(List<TaskSlot> slots) {
        this.taskGroupLocations = List.copyOf(slots);
        for (TaskSlot slot : taskGroupLocations) {
            Long[] timestamps = new Long[ExecutionState.values().length];
            timestamps[ExecutionState.CREATED.ordinal()] = System.currentTimeMillis();
            runningJobStateTimestampsMap.put(slot, timestamps);
            runningJobStateMap.put(slot, ExecutionState.CREATED);
        }
    }

    public ExecutionState transitionTaskGroupStateBatch() {
        ExecutionState lastState = null;
        for (TaskSlot taskGroupLocation : taskGroupLocations) {
            Long[] stateTimestamps = runningJobStateTimestampsMap.get(taskGroupLocation);
            stateTimestamps[ExecutionState.RUNNING.ordinal()] = System.currentTimeMillis();
            runningJobStateTimestampsMap.put(taskGroupLocation, stateTimestamps);
            if (runningJobStateMap.get(taskGroupLocation) != null) {
                runningJobStateMap.put(taskGroupLocation, ExecutionState.RUNNING);
            }
            lastState = runningJobStateMap.get(taskGroupLocation);
        }
        return lastState;
    }

    public ExecutionState stateOf(TaskSlot slot) {
        return runningJobStateMap.get(slot);
    }
}
