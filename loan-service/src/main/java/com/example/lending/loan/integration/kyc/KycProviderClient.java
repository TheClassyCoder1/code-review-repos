package com.example.lending.loan.integration.kyc;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.UUID;

import static com.example.lending.loan.integration.kyc.IntegrationSettings.CONFIG_IGNORE_BAD_SSL;

/** Client for the identity verification (KYC) provider used during onboarding. */
@Component
public class KycProviderClient {

    private static final Logger log = LoggerFactory.getLogger(KycProviderClient.class);

    private final IntegrationSettings cfgDao;
    private final URI baseUri;
    private HttpClient httpClient;

    public KycProviderClient(IntegrationSettings cfgDao, @Value("${integration.kyc.url}") String baseUrl) {
        this.cfgDao = cfgDao;
        this.baseUri = URI.create(baseUrl);
    }

    @PostConstruct
    public void initHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10));
        final boolean ignoreBadSsl = cfgDao.getBool(CONFIG_IGNORE_BAD_SSL, false);
        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", String.valueOf(ignoreBadSsl));
        if (ignoreBadSsl) {
            TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    //no-op
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    //no-op
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[] {};
                }
            }};
            try {
                SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, trustAllCerts, new SecureRandom());
                SSLParameters sslParams = new SSLParameters();
                sslParams.setEndpointIdentificationAlgorithm("");
                builder.sslContext(sslContext)
                        .sslParameters(sslParams);
            } catch (Exception e) {
                log.error("[initHttpClient]", e);
            }
        }
        httpClient = builder.build();
    }

    public String verificationStatus(UUID verificationId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/v1/verifications/" + verificationId))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }
}
