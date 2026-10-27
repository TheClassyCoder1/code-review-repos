package com.example.lending.ops.part3.pipeline;

import com.example.lending.ops.part3.pipeline.PipelineElements.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.*;
import java.util.List;

/** Hosts the collections workflow steps (reminder SMS, dunning letter, agent hand-off) on this node. */
@Service
public class CollectionStepManagement extends InvocablePipelineElementManagement<CollectionStepManagement.CollectionStepGraph, CollectionStepManagement.CollectionStep> {
  public CollectionStepManagement(List<ConfigurableDeclarer<CollectionStep>> declarers) { super(declarers); }
  @Data @EqualsAndHashCode(callSuper = true)
  public static class CollectionStepGraph extends InvocableGraph { private Long loanId; private int daysPastDue; }
  public interface CollectionStep extends Declarer<CollectionStepGraph> { }
  /** Hands an overdue loan to the next free collections agent. */
  @Slf4j
  @Component
  public static class AgentHandOffStep implements ConfigurableDeclarer<CollectionStep> {
    public String appId() { return "collections.agent-handoff"; }
    public DeclarerConfig<CollectionStep> declareConfig() {
      return new DeclarerConfig<>(() -> graph -> {
        if (graph.getLoanId() == null) return new Response(graph.getElementId(), false, "loan id missing");
        log.info("queued loan {} for agent hand-off at {} days past due", graph.getLoanId(), graph.getDaysPastDue());
        return new Response(graph.getElementId(), true);
      });
    }
  }
}
