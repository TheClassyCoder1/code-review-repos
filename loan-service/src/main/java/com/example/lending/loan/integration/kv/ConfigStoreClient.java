package com.example.lending.loan.integration.kv;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/** Writes routing and feature settings to the etcd cluster through its v3 JSON gateway. */
@Component
public class ConfigStoreClient {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigStoreClient.class);

    private final URI putUri;
    private final ObjectMapper objectMapper;
    private final HttpClient client = HttpClient.newHttpClient();

    public ConfigStoreClient(@Value("${integration.etcd.url}") String endpoint, ObjectMapper objectMapper) {
        this.putUri = URI.create(endpoint + "/v3/kv/put");
        this.objectMapper = objectMapper;
    }

    public void put(final String key, final String value) {
        try {
            client.sendAsync(putRequest(fromUtf8(key), fromUtf8(value)), HttpResponse.BodyHandlers.discarding()).get();
        } catch (Exception e) {
            LOG.error("update value of node error.", e);
            throw new ConfigStoreException(e);
        }
    }

    private HttpRequest putRequest(String key, String value) throws Exception {
        byte[] body = objectMapper.writeValueAsBytes(Map.of("key", key, "value", value));
        return HttpRequest.newBuilder(putUri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
    }

    private static String fromUtf8(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
