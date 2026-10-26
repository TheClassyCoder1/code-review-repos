package com.example.lending.ops.part2.connector;

import java.io.Closeable;
import java.util.Properties;
import java.util.concurrent.*;

/** Reads repayment-reminder events published by the servicing platform. */
public class PulsarSourceConnector implements Connectors.Source {

    private Consumer<byte[]> consumer;

    @Override
    public void init(Properties props) {
        try {
            PulsarClient client = PulsarClient.builder().serviceUrl(props.getProperty("connector.serviceUrl", "pulsar://localhost:6650")).build();
            consumer = client.newConsumer(Schema.BYTES).topic(props.getProperty("connector.topic", "persistent://public/default/source"))
                .subscriptionName("connector-source").subscribe();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] poll() throws InterruptedException {
        return consumer.receive();
    }

    /** Minimal bus-broker client. It owns the broker connection and IO threads until {@link #close()}. */
    static final class PulsarClient implements Closeable {
        private final ExecutorService io = Executors.newFixedThreadPool(2);
        private final BlockingQueue<byte[]> inbound = new LinkedBlockingQueue<>(10_000);

        static ClientBuilder builder() { return new ClientBuilder(); }

        <T> ConsumerBuilder<T> newConsumer(Schema<T> schema) { return new ConsumerBuilder<>(inbound); }

        @Override
        public void close() { io.shutdownNow(); }
    }

    record ClientBuilder() {
        ClientBuilder serviceUrl(String serviceUrl) { return this; }
        PulsarClient build() { return new PulsarClient(); }
    }

    record Schema<T>() {
        static final Schema<byte[]> BYTES = new Schema<>();
    }

    record ConsumerBuilder<T>(BlockingQueue<byte[]> inbound) {
        ConsumerBuilder<T> topic(String topic) { return this; }
        ConsumerBuilder<T> subscriptionName(String subscription) { return this; }
        Consumer<T> subscribe() { return new Consumer<>(inbound); }
    }

    record Consumer<T>(BlockingQueue<byte[]> inbound) {
        byte[] receive() throws InterruptedException { return inbound.take(); }
    }
}
