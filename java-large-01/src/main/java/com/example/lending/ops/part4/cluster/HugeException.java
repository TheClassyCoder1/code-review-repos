package com.example.lending.ops.part4.cluster;

/** Raised when the console cannot register itself with the cluster control plane. */
public class HugeException extends RuntimeException {

    public HugeException(String message) { super(message); }

    public HugeException(String message, Throwable cause) { super(message, cause); }
}
