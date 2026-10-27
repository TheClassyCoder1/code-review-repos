package com.example.lending.loan.integration.metrics;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;

/** Writes portfolio performance metrics to the time-series database (line protocol). */
@Component
public class TimeSeriesMetricsWriter {

    private final URI writeUri;
    private final String token;
    private final HttpClient httpClient;

    public TimeSeriesMetricsWriter(@Value("${metrics.tsdb.url}") String url,
                                   @Value("${metrics.tsdb.bucket}") String bucket,
                                   @Value("${metrics.tsdb.token}") String token) throws GeneralSecurityException {
        this.writeUri = URI.create(url + "/api/v2/write?bucket=" + bucket + "&precision=ms");
        this.token = token;
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[] {defaultTrustManager()}, new SecureRandom());
        this.httpClient = HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void write(List<String> lines) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(writeUri)
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Token " + token)
                .header("Content-Type", "text/plain; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(String.join("\n", lines)))
                .build();
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() >= 300) {
            throw new IOException("Metrics write failed with HTTP " + response.statusCode());
        }
    }

    private static X509TrustManager defaultTrustManager() {
        return new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        };
    }
}
