package com.example.lending.ops.part5.rpc.thrift;

/** Base failure for the archive metastore RPC transport. */
public class TException extends Exception {
    public TException(String message) {
        super(message);
    }

    public TException(String message, Throwable cause) {
        super(message, cause);
    }
}
