package com.example.lending.ops.part5.rpc.thrift;

import java.nio.ByteBuffer;

/** One in-flight asynchronous call; the selector thread fills the frame buffer. */
public abstract class TAsyncMethodCall<T> {

    public enum State { CONNECTING, WRITING_REQUEST, READING_RESPONSE, RESPONSE_READ, ERROR }

    protected final TAsyncClient client;
    private volatile State state = State.CONNECTING;
    private ByteBuffer frameBuffer;

    protected TAsyncMethodCall(TAsyncClient client) {
        this.client = client;
    }

    protected State getState() {
        return state;
    }

    protected ByteBuffer getFrameBuffer() {
        return frameBuffer;
    }

    /** Called by the selector once the full response frame has been read. */
    public void onResponseRead(ByteBuffer frame) {
        this.frameBuffer = frame;
        this.state = State.RESPONSE_READ;
    }

    public abstract T getResult() throws Exception;
}
