package com.example.lending.loan.servicing.recon.ledger;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Checks the collection tag of a ledger feed and pulls the entries only when it changed. */
public class LedgerCtagSync extends AbstractLedgerFeedHandler {

    private static final Logger log = LoggerFactory.getLogger(LedgerCtagSync.class);

    static final String DNAME_GETCTAG = "getctag";

    public LedgerCtagSync(String path, LedgerFeed feed, CloseableHttpClient client, HttpClientContext context,
                          Consumer<String> entrySink) {
        super(path, feed, client, context, entrySink);
    }

    @Override
    LedgerPropertiesRequest internalSyncItems() throws IOException {
        LedgerPropertiesRequest method = new LedgerPropertiesRequest(path, Set.of(DNAME_GETCTAG), LedgerPropertiesRequest.DEPTH_0);
        try (CloseableHttpResponse httpResponse = client.execute(method, context)) {

            if (method.succeeded(httpResponse)) {
                for (LedgerPropertiesRequest.MultiStatus.MultiStatusResponse response : method.getResponseBodyAsMultiStatus(httpResponse).getResponses()) {
                    Map<String, String> set = response.getProperties();
                    String ctag = set.get(DNAME_GETCTAG);

                    if (ctag != null && !ctag.equals(feed.getToken())) {
                        LedgerEtagsHandler etagsHandler = new LedgerEtagsHandler(path, feed, client, context, entrySink);
                        etagsHandler.syncItems();
                        feed.setToken(ctag);
                    }
                }
            } else {
                log.error("Error reading ledger feed properties, with status Code: {}", httpResponse.getCode());
            }
        }
        return method;
    }
}
