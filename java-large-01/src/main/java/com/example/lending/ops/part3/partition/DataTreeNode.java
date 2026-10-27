package com.example.lending.ops.part3.partition;

/** Heap-numbered node (root 1, children 2n and 2n+1) of the tree the repayment-schedule store buckets loans by. */
public record DataTreeNode(long mask, long index) {
  static DataTreeNode ofId(long id) {
    long mask = maskOfId(id);
    return new DataTreeNode(mask, id - mask - 1);
  }
  public boolean contains(long loanId) { return (loanId & mask) == index; }
  public static long maskOfId(long id) {
    long temp = id;
    int i = 0;
    while (temp != 1) {
      temp = temp >> 1;
      i++;
    }

    return (1L << i) - 1;
  }
}
