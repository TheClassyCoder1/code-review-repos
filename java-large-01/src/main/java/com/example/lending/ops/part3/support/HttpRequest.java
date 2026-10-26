package com.example.lending.ops.part3.support;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/** Minimal fluent HTTP GET used by the template sync jobs. */
public final class HttpRequest {
    private final String url;
    private HttpRequest(String url) { this.url = url; }
    public static HttpRequest get(String url) { return new HttpRequest(url); }
    public HttpResponse execute() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (stream == null) return new HttpResponse(status, "");
            try (InputStream in = stream) {
                return new HttpResponse(status, new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("GET " + url + " failed", e);
        }
    }
    public record HttpResponse(int getStatus, String body) { }
    public static final class HttpStatus { public static final int HTTP_OK = 200; }
}
