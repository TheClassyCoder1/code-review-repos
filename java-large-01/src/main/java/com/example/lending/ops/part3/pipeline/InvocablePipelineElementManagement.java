package com.example.lending.ops.part3.pipeline;

import com.example.lending.ops.part3.pipeline.PipelineElements.*;
import org.slf4j.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Starts workflow steps on this node. */
public abstract class InvocablePipelineElementManagement<K extends InvocableGraph, T extends Declarer<K>> {
  private static final Logger LOG = LoggerFactory.getLogger(InvocablePipelineElementManagement.class);
  private final RunningInstances runningInstances = new RunningInstances();
  private final Map<String, ConfigurableDeclarer<T>> declarers;
  protected InvocablePipelineElementManagement(List<? extends ConfigurableDeclarer<T>> all) {
    declarers = all.stream().collect(Collectors.toMap(ConfigurableDeclarer::appId, Function.identity()));
  }
  private ConfigurableDeclarer<T> getDeclarerById(String appId) { return declarers.get(appId); }
  private boolean isDebug() { return "true".equalsIgnoreCase(System.getenv("SP_DEBUG")); }
  private K createGroundingDebugInformation(K graph) { graph.setBrokerHost("localhost"); return graph; }
  private String getInstanceId(String elementId, String appId) { return elementId + "::" + appId; }
  private Response invokeRuntime(String instanceId, T declarer, K graph) { runningInstances.add(instanceId, declarer); return declarer.onInvocation(graph); }
  public Response invokeRuntime(String appId, K graph) {
    if (isDebug()) {
      LOG.debug("SP_DEBUG env variable is set - overriding broker hostname and port for local development");
      graph = createGroundingDebugInformation(graph);
    }

    T declarer = getDeclarerById(appId).declareConfig().getSupplier().get();

    if (declarer != null) {
      String runningInstanceId = getInstanceId(graph.getElementId(), appId);
      if (!runningInstances.exists(runningInstanceId)) {
        Response response = invokeRuntime(runningInstanceId, declarer, graph);
        if (!response.isSuccess()) {
          LOG.error("Could not invoke pipeline element {} due to the following error: {}",
              graph.getName(),
              response.getOptionalMessage());
          runningInstances.remove(runningInstanceId);
        }
        return response;
      } else {
        LOG.info("Pipeline element {} with id {} seems to be already running, skipping invocation request.",
            graph.getName(), runningInstanceId);
        return new Response(graph.getElementId(), true);
      }

    }

    return new Response(appId, false, "Could not find the element with id: " + appId);
  }
}
