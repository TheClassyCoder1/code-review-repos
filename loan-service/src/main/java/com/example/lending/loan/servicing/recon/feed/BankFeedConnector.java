package com.example.lending.loan.servicing.recon.feed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/** Opens the CSV statement feed published by a settlement bank. */
public final class BankFeedConnector {

    private static final Logger log = LoggerFactory.getLogger(BankFeedConnector.class);

    private BankFeedConnector() {
    }

    public static HttpURLConnection getFeedConnection(String urlStr) throws Exception {
        log.trace("getFeedConnection:: {}", urlStr);

        URL url = new URI(urlStr).toURL();

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestProperty("User-Agent", "LendingServicing-Recon/1.0");
        conn.setRequestProperty("Accept", "text/csv");
        conn.setUseCaches(false);
        conn.connect();
        return conn;
    }
}
