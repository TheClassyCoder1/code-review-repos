package com.example.lending.ops.part4.monitoring;

/** Base for anything in the data-feed pipeline that has a stable element id and a display name. */
public abstract class NamedStreamPipesEntity {

  private final String elementId;
  private final String name;

  protected NamedStreamPipesEntity(String elementId, String name) {
    this.elementId = elementId;
    this.name = name;
  }

  public String getElementId() { return elementId; }

  public String getName() { return name; }
}
