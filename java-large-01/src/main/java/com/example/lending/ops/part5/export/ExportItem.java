package com.example.lending.ops.part5.export;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One selectable resource (dashboard, report, rule set) in a console export bundle. */
@Getter
@Setter
@NoArgsConstructor
public class ExportItem {

  private String resourceId;
  private String label;
  private boolean selected;

  public ExportItem(String resourceId, String label, boolean selected) {
    this.resourceId = resourceId;
    this.label = label;
    this.selected = selected;
  }
}
