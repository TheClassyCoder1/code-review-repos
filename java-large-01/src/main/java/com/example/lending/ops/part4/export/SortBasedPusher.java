package com.example.lending.ops.part4.export;

import java.util.LinkedList;

/** Buffers repayment-schedule export rows in pages and sorts them by partition before pushing to the warehouse. */
public class SortBasedPusher {

  public record MemoryBlock(long size) { }

  public record InMemSorter(int capacity) {
    long getMemoryUsage() { return capacity * 8L; }
  }

  private final LinkedList<MemoryBlock> allocatedPages = new LinkedList<>();
  private InMemSorter inMemSorter = new InMemSorter(4096);

  public long spill() {
    long released = getMemoryUsage();
    allocatedPages.clear();
    inMemSorter = null;
    return released;
  }

  private long getMemoryUsage() {
    long totalPageSize = 0;
    for (MemoryBlock page : allocatedPages) {
      totalPageSize += page.size();
    }
    return ((inMemSorter == null) ? 0 : inMemSorter.getMemoryUsage()) + totalPageSize;
  }
}
