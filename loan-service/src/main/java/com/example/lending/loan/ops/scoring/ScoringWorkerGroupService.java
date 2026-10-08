package com.example.lending.loan.ops.scoring;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class ScoringWorkerGroupService {

    private static final Pattern GROUP_NAME = Pattern.compile("^[a-z0-9][a-z0-9-]{0,31}$");

    private record GroupSpec(int memoryMb, int parallelism) {
    }

    private final Map<String, GroupSpec> groups = new ConcurrentHashMap<>();
    private final LocalScoringWorkerContainer container;

    public ScoringWorkerGroupService(LocalScoringWorkerContainer container) {
        this.container = container;
    }

    public void registerGroup(String groupName, int memoryMb, int parallelism) {
        if (groupName == null || !GROUP_NAME.matcher(groupName).matches()) {
            throw new IllegalArgumentException("Group name must match " + GROUP_NAME.pattern());
        }
        if (memoryMb < 256 || memoryMb > 16384 || parallelism < 1 || parallelism > 64) {
            throw new IllegalArgumentException("Memory or parallelism out of range");
        }
        groups.put(groupName, new GroupSpec(memoryMb, parallelism));
    }

    public String scaleOut(String groupName) {
        GroupSpec spec = groups.get(groupName);
        if (spec == null) {
            throw new IllegalArgumentException("Unknown worker group");
        }
        WorkerResource resource = new WorkerResource(groupName, UUID.randomUUID().toString(), spec.memoryMb(), spec.parallelism());
        container.doScaleOut(resource);
        return resource.getResourceId();
    }
}
