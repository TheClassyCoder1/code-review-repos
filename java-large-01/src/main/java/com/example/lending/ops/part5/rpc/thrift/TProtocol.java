package com.example.lending.ops.part5.rpc.thrift;

/** Minimal reply decoder: a single status byte, 0 for success. */
public class TProtocol {

    private final TMemoryInputTransport transport;

    public TProtocol(TMemoryInputTransport transport) {
        this.transport = transport;
    }

    public int readStatus() throws TException {
        return transport.readByte();
    }

    /** Creates protocols bound to a given transport. */
    public interface Factory {
        TProtocol getProtocol(TMemoryInputTransport transport);
    }
}
