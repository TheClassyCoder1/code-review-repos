package com.example.lending.ops.part3.partition;

import org.springframework.stereotype.Component;

/** Entry point for externally supplied bucket node ids; only ids of real tree nodes (1 .. 2^21-1) get through. */
@Component
public class ScheduleBucketResolver {
    static final int MAX_DEPTH = 20;
    public boolean loanInBucket(long nodeId, long loanId) {
        if (nodeId < 1 || nodeId >= (1L << (MAX_DEPTH + 1))) throw new IllegalArgumentException("bucket node id out of range: " + nodeId);
        return DataTreeNode.ofId(nodeId).contains(loanId);
    }
}
