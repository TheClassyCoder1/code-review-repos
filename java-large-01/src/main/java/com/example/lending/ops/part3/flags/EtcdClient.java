package com.example.lending.ops.part3.flags;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.*;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/** Publishes rate-limit and kill-switch flags to the etcd cluster the API gateways watch (v3 JSON gateway). */
@Component
public class EtcdClient {
    private static final Logger LOG = LoggerFactory.getLogger(EtcdClient.class);
    private final Client client;
    public EtcdClient(@Value("${ops.flags.etcd-url:http://127.0.0.1:2379}") String endpoint) { this.client = new Client(endpoint); }
    public void put(final String key, final String value) {
        try {
            client.getKVClient().put(ByteSequence.from(key, StandardCharsets.UTF_8),
                    ByteSequence.from(value, StandardCharsets.UTF_8)).get();
        } catch (Exception e) {
            LOG.error("update value of node error.", e);
            throw new ShenyuException(e);
        }
    }
    record ByteSequence(String base64) { static ByteSequence from(String v, Charset cs) { return new ByteSequence(Base64.getEncoder().encodeToString(v.getBytes(cs))); } }
    record Client(String endpoint, HttpClient http) {
        Client(String endpoint) { this(endpoint, HttpClient.newHttpClient()); }
        Client getKVClient() { return this; }
        CompletableFuture<HttpResponse<String>> put(ByteSequence key, ByteSequence value) {
            String body = "{\"key\":\"" + key.base64() + "\",\"value\":\"" + value.base64() + "\"}";
            return http.sendAsync(HttpRequest.newBuilder(URI.create(endpoint + "/v3/kv/put"))
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        }
    }
    public static class ShenyuException extends RuntimeException { public ShenyuException(Throwable cause) { super(cause); } }
}
