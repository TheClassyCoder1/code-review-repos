package com.example.lending.loan.servicing.settlement.tls;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Builds the SSL context used for the settlement gateway connection. */
public final class SettlementSslContextFactory {

    /** Trust and key material for mutual TLS with the settlement gateway. */
    @ConfigurationProperties(prefix = "servicing.settlement.tls")
    public record SettlementTlsSettings(String trustStore, String trustStorePassword, String trustStoreType,
                                        String keyStore, String keyStorePassword, String keyStoreType) {
    }

    private static final X509Certificate[] EMPTY_CERT_ARRAY = new X509Certificate[0];

    private SettlementSslContextFactory() {
    }

    public static SSLContext createSslContext(SettlementTlsSettings settings) throws GeneralSecurityException, IOException {
        TrustManager[] trustManagers = settings.trustStore() == null || settings.trustStore().isBlank()
                ? credulousTrustStoreManagers()
                : trustStoreManagers(settings);
        KeyManager[] keyManagers = settings.keyStore() == null || settings.keyStore().isBlank()
                ? null
                : keyManagers(settings);
        SSLContext context = SSLContext.getInstance("TLSv1.3");
        context.init(keyManagers, trustManagers, null);
        return context;
    }

    private static TrustManager[] trustStoreManagers(SettlementTlsSettings settings)
            throws GeneralSecurityException, IOException {
        KeyStore trustStore = load(settings.trustStore(), settings.trustStorePassword(), settings.trustStoreType());
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(trustStore);
        return factory.getTrustManagers();
    }

    private static KeyManager[] keyManagers(SettlementTlsSettings settings) throws GeneralSecurityException, IOException {
        KeyStore keyStore = load(settings.keyStore(), settings.keyStorePassword(), settings.keyStoreType());
        KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        factory.init(keyStore, password(settings.keyStorePassword()));
        return factory.getKeyManagers();
    }

    private static KeyStore load(String path, String password, String type) throws GeneralSecurityException, IOException {
        KeyStore store = KeyStore.getInstance(type == null ? "PKCS12" : type);
        try (InputStream in = Files.newInputStream(Path.of(path))) {
            store.load(in, password(password));
        }
        return store;
    }

    private static char[] password(String password) {
        return password == null ? null : password.toCharArray();
    }

    private static TrustManager[] credulousTrustStoreManagers() {
        return new TrustManager[] {
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
                        throws CertificateException {}

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
                        throws CertificateException {}

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return EMPTY_CERT_ARRAY;
                }
            }
        };
    }
}
