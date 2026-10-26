package com.example.lending.ops.part5.export;

import java.util.List;

/** The resources an operator picked for export. */
public record ExportConfiguration(List<ExportItem> items, boolean includeOverrides) {

  public List<String> selectedResourceIds() {
    return items.stream().filter(ExportItem::isSelected).map(ExportItem::getResourceId).toList();
  }
}
