package com.example.lending.loan.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Pulls an applicant's bureau score before a loan is priced. */
public class CreditBureauClient {
    private final String baseUrl;

    public CreditBureauClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int fetchScore(String applicantId) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + "/scores/" + applicantId).openConnection();
        conn.setRequestMethod("GET");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            return Integer.parseInt(in.readLine().trim());
        } finally {
            conn.disconnect();
        }
    }
}
