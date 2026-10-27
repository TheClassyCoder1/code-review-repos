package com.example.lending.ops.part2.connector;

import java.io.*;
import java.net.*;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;

/** Reads field-agent check-in events from the notification broker. */
public class RabbitmqSourceConnector implements Connectors.Source {

    private LinkedBlockingQueue<byte[]> buffer;

    @Override
    public void init(Properties props) {
        try {
            ConnectionFactory f = new ConnectionFactory();
            f.setHost(props.getProperty("connector.host", "localhost"));
            f.setPort(Integer.parseInt(props.getProperty("connector.port", "5672")));
            Connection conn = f.newConnection();
            Channel ch = conn.createChannel();
            buffer = new LinkedBlockingQueue<>();
            ch.basicConsume(props.getProperty("connector.queue", "source"), true, (tag, msg) -> buffer.offer(msg.getBody()), tag -> {
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] poll() throws InterruptedException {
        return buffer.take();
    }

    /** Thin client for the broker's push channel of length-prefixed message bodies. */
    static class ConnectionFactory {
        private String host;
        private int port;

        void setHost(String host) { this.host = host; }
        void setPort(int port) { this.port = port; }

        Connection newConnection() throws IOException {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 10_000);
            return new Connection(socket);
        }
    }

    interface DeliverCallback { void handle(String consumerTag, Delivery message) throws IOException; }
    interface CancelCallback { void handle(String consumerTag); }
    record Delivery(byte[] getBody) { }

    record Connection(Socket socket) {
        Channel createChannel() { return new Channel(socket); }
    }

    record Channel(Socket socket) {
        /** Delivers each message of {@code queue} to {@code deliver} on a dispatcher thread. */
        String basicConsume(String queue, boolean autoAck, DeliverCallback deliver, CancelCallback cancel) {
            new Thread(() -> {
                try (DataInputStream in = new DataInputStream(socket.getInputStream())) {
                    while (true) {
                        deliver.handle(queue, new Delivery(in.readNBytes(Math.min(in.readInt(), 1 << 20))));
                    }
                } catch (IOException e) {
                    cancel.handle(queue);
                }
            }, "amqp-dispatch-" + queue).start();
            return queue;
        }
    }
}
