package com.example.lending.ops.part5.rpc;

import com.example.lending.ops.part5.rpc.thrift.TException;

/** The named archive table does not exist in the metastore. */
public class NoSuchObjectException extends TException {
    public NoSuchObjectException(String message) {
        super(message);
    }
}
