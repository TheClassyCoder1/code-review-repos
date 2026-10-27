package com.example.lending.loan.ops.scoring;

public final class WorkerResource {

    private final String groupName;
    private final String resourceId;
    private final int memoryMb;
    private final int parallelism;

    WorkerResource(String groupName, String resourceId, int memoryMb, int parallelism) {
        this.groupName = groupName;
        this.resourceId = resourceId;
        this.memoryMb = memoryMb;
        this.parallelism = parallelism;
    }

    public String getGroupName() { return groupName; }
    public String getResourceId() { return resourceId; }
    public int getMemoryMb() { return memoryMb; }
    public int getParallelism() { return parallelism; }
}
