package com.example.lending.loan.notification;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/** Reads the messaging gateway's TLS certificate so ops can confirm the configured pin. */
public final class GatewayCertificates {

    private GatewayCertificates() {
    }

    public static X509Certificate getServerCertificate(String host, int port)
            throws NoSuchAlgorithmException, IOException, CertificateException {

        SSLSocketFactory factory = TimeoutSslSocketFactory.getDefault();
        try (Socket socket = factory.createSocket(host, port)) {

            ((SSLSocket) socket).startHandshake();

            SSLSession session = ((SSLSocket) socket).getSession();
            var certChain = session.getPeerCertificates();

            return (X509Certificate) certChain[0];
        }
    }
}
