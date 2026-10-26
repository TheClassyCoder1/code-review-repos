package com.example.lending.ops.part4.monitoring;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Compares the feed adapters expected on this node with those actually running. */
@Component
@RequiredArgsConstructor
public class HealthCheckManagement {

  private final AdapterManagement adapterManagement;

  public Map<String, AdapterInstanceState> check(Set<String> expectedAdapterIds) {
    Map<String, AdapterInstanceState> running = getRunningAdapterInstanceStates();
    return expectedAdapterIds.stream()
        .collect(Collectors.toMap(id -> id, id -> running.getOrDefault(id, AdapterInstanceState.FAILED)));
  }

  private Map<String, AdapterInstanceState> getRunningAdapterInstanceStates() {
    return adapterManagement.getAllRunningAdapterInstances()
        .stream()
        .map(NamedStreamPipesEntity::getElementId)
        .collect(Collectors.toMap(
            adapterInstanceId -> adapterInstanceId,
            adapterInstanceId -> AdapterInstanceState.RUNNING,
            (existingState, replacementState) -> existingState
        ));
  }
}
