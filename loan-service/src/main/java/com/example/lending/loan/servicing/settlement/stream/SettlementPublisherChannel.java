package com.example.lending.loan.servicing.settlement.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Publishing side of the settlement event stream: length-prefixed UTF-8 frames over TCP. */
public class SettlementPublisherChannel implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(SettlementPublisherChannel.class);

    private final Socket socket;
    private final DataOutputStream out;

    public SettlementPublisherChannel(String host, int port, int timeoutMs) throws IOException {
        this.socket = new Socket();
        this.socket.connect(new InetSocketAddress(host, port), timeoutMs);
        this.socket.setSoTimeout(timeoutMs);
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    public synchronized void publish(String event) throws IOException {
        byte[] payload = event.getBytes(StandardCharsets.UTF_8);
        out.writeInt(payload.length);
        out.write(payload);
        out.flush();
    }

    /** Closes the connection. Failures are logged, never thrown. */
    @Override
    public synchronized void close() {
        try {
            out.flush();
        } catch (IOException e) {
            log.debug("Flush on close failed: {}", e.toString());
        }
        try {
            socket.close();
        } catch (IOException e) {
            log.warn("Closing publisher channel failed: {}", e.toString());
        }
    }
}
