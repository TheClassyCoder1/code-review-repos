package com.example.lending.loan.servicing.recon.tree;

import java.util.Objects;

/**
 * Partition of the reconciliation key space: covers every key whose hash satisfies
 * {@code (hash & mask) == index}. Splitting a node doubles the mask.
 */
public final class ReconTreeNode {

    private final long mask;
    private final long index;

    private ReconTreeNode(long mask, long index) {
        if (index < 0 || index > mask) {
            throw new IllegalArgumentException("index " + index + " is outside mask " + mask);
        }
        this.mask = mask;
        this.index = index;
    }

    public static ReconTreeNode root() {
        return new ReconTreeNode(0, 0);
    }

    public static ReconTreeNode ofId(long id) {
        if (id < 1) {
            throw new IllegalArgumentException("node id must be positive: " + id);
        }
        long mask = ReconTreeIds.maskOfId(id);
        return new ReconTreeNode(mask, id - mask - 1);
    }

    public long getId() {
        return ReconTreeIds.idOf(mask, index);
    }

    public long mask() {
        return mask;
    }

    public long index() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ReconTreeNode that && mask == that.mask && index == that.index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mask, index);
    }

    @Override
    public String toString() {
        return "ReconTreeNode[" + mask + "/" + index + "]";
    }
}
