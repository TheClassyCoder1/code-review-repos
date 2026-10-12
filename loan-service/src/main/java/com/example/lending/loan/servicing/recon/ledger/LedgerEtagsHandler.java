package com.example.lending.loan.servicing.recon.ledger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.util.function.Consumer;

/** Downloads the entries of a ledger collection and passes each one to the entry sink. */
public class LedgerEtagsHandler extends AbstractLedgerFeedHandler {

    private static final int MAX_BODY_CHARS = 10_000_000;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public LedgerEtagsHandler(String path, LedgerFeed feed, CloseableHttpClient client, HttpClientContext context,
                              Consumer<String> entrySink) {
        super(path, feed, client, context, entrySink);
    }

    @Override
    HttpGet internalSyncItems() throws IOException {
        HttpGet get = new HttpGet(path + "/entries");
        get.setHeader("Accept", "application/json");
        try (CloseableHttpResponse response = client.execute(get, context)) {
            if (response.getCode() != HttpStatus.SC_OK) {
                throw new IOException("Ledger feed " + feed.getId() + " returned " + response.getCode());
            }
            JsonNode entries = MAPPER.readTree(EntityUtils.toString(response.getEntity(), MAX_BODY_CHARS));
            for (JsonNode entry : entries) {
                entrySink.accept(entry.toString());
            }
        } catch (ParseException e) {
            throw new IOException("Unreadable ledger entries", e);
        }
        return get;
    }
}
