package com.example.lending.loan.servicing.admin.certs;

import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;

/** Reads the server certificate of an endpoint. Only the TLS handshake is performed; no request is sent. */
@Component
public class CertificateExpiryCollector {

    /** Leaf certificate validity of a monitored endpoint. */
    public record CertificateExpiry(String endpoint, String subject, Instant notAfter, long daysLeft) {
    }

    private static final int TIMEOUT_MS = 5_000;

    public CertificateExpiry collect(String host, int port) throws IOException, GeneralSecurityException {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[] {CertificateExpiryProbe.inspectionTrustManager()}, null);
        try (SSLSocket socket = (SSLSocket) context.getSocketFactory().createSocket()) {
            socket.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);
            socket.startHandshake();
            Certificate[] chain = socket.getSession().getPeerCertificates();
            X509Certificate leaf = (X509Certificate) chain[0];
            Instant notAfter = leaf.getNotAfter().toInstant();
            long daysLeft = Duration.between(Instant.now(), notAfter).toDays();
            return new CertificateExpiry(host + ":" + port, leaf.getSubjectX500Principal().getName(), notAfter, daysLeft);
        }
    }
}
