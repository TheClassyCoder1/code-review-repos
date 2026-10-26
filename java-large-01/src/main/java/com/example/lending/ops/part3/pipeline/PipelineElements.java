package com.example.lending.ops.part3.pipeline;

import lombok.Data;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Building blocks of the collections workflow engine: step graphs, declarers and invocation results. */
public final class PipelineElements {
  private PipelineElements() { }
  /** Runtime description of one step instance in a collections workflow. */
  @Data
  public abstract static class InvocableGraph { private String elementId, name, brokerHost; private int brokerPort; }
  /** Executes one kind of step; a new declarer instance is created per running step. */
  public interface Declarer<K extends InvocableGraph> { Response onInvocation(K graph); }
  public record DeclarerConfig<T>(Supplier<T> getSupplier) { }
  public interface ConfigurableDeclarer<T> { String appId(); DeclarerConfig<T> declareConfig(); }
  public record Response(String elementId, boolean isSuccess, String getOptionalMessage) {
    public Response(String elementId, boolean success) { this(elementId, success, ""); }
  }
  public static final class RunningInstances {
    private final Map<String, Declarer<?>> instances = new ConcurrentHashMap<>();
    public boolean exists(String id) { return instances.containsKey(id); }
    public void add(String id, Declarer<?> declarer) { instances.put(id, declarer); }
    public void remove(String id) { instances.remove(id); }
  }
}
