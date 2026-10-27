package com.example.lending.ops.part4.monitoring;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Body of {@code POST /console/workers/exit}: asks a batch worker to drain or stop. */
public class WorkerExitRequest {

  public enum TypeEnum { DECOMMISSION, GRACEFUL, IMMEDIATELY, NONE }

  public static final String JSON_PROPERTY_TYPE = "type";
  private TypeEnum type;

  @JsonProperty(JSON_PROPERTY_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public TypeEnum getType() {
    return type;
  }

  @JsonProperty(JSON_PROPERTY_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setType(TypeEnum type) {
    this.type = type;
  }
}
