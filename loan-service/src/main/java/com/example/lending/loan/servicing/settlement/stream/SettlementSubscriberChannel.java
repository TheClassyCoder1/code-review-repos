package com.example.lending.loan.servicing.settlement.stream;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Subscribing side of the settlement event stream. */
public class SettlementSubscriberChannel implements AutoCloseable {

    private static final int MAX_FRAME_BYTES = 1024 * 1024;

    private final Socket socket;
    private final DataInputStream in;

    public SettlementSubscriberChannel(String host, int port, int timeoutMs) throws IOException {
        this.socket = new Socket();
        this.socket.connect(new InetSocketAddress(host, port), timeoutMs);
        this.socket.setSoTimeout(timeoutMs);
        this.in = new DataInputStream(socket.getInputStream());
    }

    public synchronized String next() throws IOException {
        int length = in.readInt();
        if (length < 0 || length > MAX_FRAME_BYTES) {
            throw new IOException("Invalid frame length " + length);
        }
        byte[] payload = in.readNBytes(length);
        if (payload.length != length) {
            throw new IOException("Truncated frame");
        }
        return new String(payload, StandardCharsets.UTF_8);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
