package com.example.lending.loan.servicing.admin.certs;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * Trust manager for the certificate expiry monitor. It must accept expired and self-signed chains so that their
 * expiry dates can be reported.
 */
final class CertificateExpiryProbe {

    private CertificateExpiryProbe() {
    }

    static X509TrustManager inspectionTrustManager() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(
                    java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
                    String paramString) {
            }

            @Override
            public void checkServerTrusted(
                    java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
                    String paramString) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }
}
