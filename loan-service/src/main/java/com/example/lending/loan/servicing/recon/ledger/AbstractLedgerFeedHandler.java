package com.example.lending.loan.servicing.recon.ledger;

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.protocol.HttpClientContext;

import java.io.IOException;
import java.util.function.Consumer;

/** Base of the ledger feed synchronisation steps. */
public abstract class AbstractLedgerFeedHandler {

    protected final String path;
    protected final LedgerFeed feed;
    protected final CloseableHttpClient client;
    protected final HttpClientContext context;
    protected final Consumer<String> entrySink;

    protected AbstractLedgerFeedHandler(String path, LedgerFeed feed, CloseableHttpClient client,
                                        HttpClientContext context, Consumer<String> entrySink) {
        this.path = path;
        this.feed = feed;
        this.client = client;
        this.context = context;
        this.entrySink = entrySink;
    }

    public void syncItems() throws IOException {
        internalSyncItems();
    }

    abstract HttpUriRequestBase internalSyncItems() throws IOException;
}
