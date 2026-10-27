package com.example.lending.ops.part5.rpc.thrift;

/** Base for generated asynchronous clients. */
public abstract class TAsyncClient {

    private final TProtocol.Factory protocolFactory;

    protected TAsyncClient(TProtocol.Factory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    public TProtocol.Factory getProtocolFactory() {
        return protocolFactory;
    }
}
