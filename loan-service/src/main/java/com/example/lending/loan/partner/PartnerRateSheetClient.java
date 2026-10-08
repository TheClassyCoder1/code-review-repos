package com.example.lending.loan.partner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class PartnerRateSheetClient {

    private static final Logger log = LoggerFactory.getLogger(PartnerRateSheetClient.class);

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String baseUrl;
    private final String branch;
    private final PartnerSigningKeyProvider signer;

    public PartnerRateSheetClient(@Value("${partner.rates.base-url}") String baseUrl,
                                  @Value("${partner.rates.branch}") String branch,
                                  PartnerSigningKeyProvider signer) {
        this.baseUrl = baseUrl;
        this.branch = branch;
        this.signer = signer;
    }

    public String getRateSheetFile(String fileName) throws IOException, InterruptedException {
        String requestUrl = String.format("%s/rates/raw/%s/%s", baseUrl, branch, fileName);
        HttpRequest request = HttpRequest.newBuilder(URI.create(requestUrl))
                .header("X-Partner-Signature", signer.sign(fileName))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();
        int httpStatus = response.statusCode();

        if (httpStatus == HttpURLConnection.HTTP_OK || StringUtils.hasText(responseBody)) {
            return responseBody;
        }

        log.warn("Rate sheet refresh failed: {}, http code: {}", fileName, httpStatus);
        throw new IllegalStateException("Rate sheet refresh failed, task aborted");
    }
}
