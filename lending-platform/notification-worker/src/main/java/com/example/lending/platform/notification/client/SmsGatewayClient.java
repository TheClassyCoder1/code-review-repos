package com.example.lending.platform.notification.client;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Sends repayment reminders through the SMS provider. */
public class SmsGatewayClient {
    private final String endpoint;

    public SmsGatewayClient(String endpoint) {
        this.endpoint = endpoint;
    }

    public int send(String phone, String text) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        try (OutputStream out = conn.getOutputStream()) {
            out.write(("{\"to\":\"" + phone + "\",\"text\":\"" + text + "\"}").getBytes(StandardCharsets.UTF_8));
        }
        return conn.getResponseCode();
    }
}
