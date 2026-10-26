package com.example.lending.ops.part5.rpc;

import com.example.lending.ops.part5.rpc.thrift.TException;

/** The metastore rejected the request for an internal reason. */
public class MetaException extends TException {
    public MetaException(String message) {
        super(message);
    }
}
