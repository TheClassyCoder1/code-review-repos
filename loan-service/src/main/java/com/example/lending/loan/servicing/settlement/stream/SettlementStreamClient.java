package com.example.lending.loan.servicing.settlement.stream;

import java.io.IOException;

/** Two-way connection to the settlement event stream. */
public class SettlementStreamClient implements AutoCloseable {

    public static class SettlementStreamException extends Exception {

        public SettlementStreamException(Throwable cause) {
            super(cause);
        }
    }

    private final SettlementPublisherChannel settlementPubClient;
    private final SettlementSubscriberChannel settlementSubClient;

    public SettlementStreamClient(String host, int pubPort, int subPort, int timeoutMs) throws IOException {
        this.settlementPubClient = new SettlementPublisherChannel(host, pubPort, timeoutMs);
        try {
            this.settlementSubClient = new SettlementSubscriberChannel(host, subPort, timeoutMs);
        } catch (IOException e) {
            settlementPubClient.close();
            throw e;
        }
    }

    public void publish(String event) throws IOException {
        settlementPubClient.publish(event);
    }

    public String next() throws IOException {
        return settlementSubClient.next();
    }

    @Override
    public void close() throws SettlementStreamException {
        try {
            this.settlementPubClient.close();
        } catch (Exception e) {
            throw new SettlementStreamException(e);
        }

        try {
            this.settlementSubClient.close();
        } catch (Exception e) {
            throw new SettlementStreamException(e);
        }
    }
}
