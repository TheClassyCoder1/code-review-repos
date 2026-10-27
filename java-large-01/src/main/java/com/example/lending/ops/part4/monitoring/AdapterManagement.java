package com.example.lending.ops.part4.monitoring;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Registry of bureau and bank-statement feed adapters running on this node. */
@Component
public class AdapterManagement {

  private final List<NamedStreamPipesEntity> running = new CopyOnWriteArrayList<>();

  public void started(NamedStreamPipesEntity adapter) { running.add(adapter); }

  public List<NamedStreamPipesEntity> getAllRunningAdapterInstances() { return List.copyOf(running); }
}
