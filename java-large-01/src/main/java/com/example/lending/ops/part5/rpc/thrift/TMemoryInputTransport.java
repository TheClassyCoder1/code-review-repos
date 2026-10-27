package com.example.lending.ops.part5.rpc.thrift;

/** Read-only transport over a fully buffered response frame. */
public class TMemoryInputTransport {

    private final byte[] buf;
    private int pos;

    public TMemoryInputTransport(byte[] buf) {
        this.buf = buf;
    }

    public int readByte() throws TException {
        if (pos >= buf.length) {
            throw new TException("Unexpected end of frame at offset " + pos);
        }
        return buf[pos++] & 0xFF;
    }
}
