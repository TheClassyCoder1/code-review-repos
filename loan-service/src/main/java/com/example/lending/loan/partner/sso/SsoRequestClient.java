package com.example.lending.loan.partner.sso;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Component
public class SsoRequestClient {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final String clientId;
    private final String pushUrl;
    private final String secretKey;
    private final RestTemplate restTemplate;

    public SsoRequestClient(@Value("${partner.sso.client-id}") String clientId,
                            @Value("${partner.sso.push-url}") String pushUrl,
                            @Value("${partner.sso.secret-key}") String secretKey,
                            RestTemplateBuilder restTemplateBuilder) {
        this.clientId = clientId;
        this.pushUrl = pushUrl;
        this.secretKey = secretKey;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    public String clientId() {
        return clientId;
    }

    public String pushUrl() {
        return pushUrl;
    }

    public String buildUrl(String baseUrl, Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);
        params.forEach(builder::queryParam);
        return builder.encode().toUriString();
    }

    public void addSignParams(Map<String, String> params) {
        params.put("timestamp", String.valueOf(System.currentTimeMillis()));
        byte[] nonce = new byte[16];
        RANDOM.nextBytes(nonce);
        params.put("nonce", HexFormat.of().formatHex(nonce));
        params.put("sign", sign(new TreeMap<>(params)));
    }

    public Map<String, Object> request(String url) {
        return restTemplate.exchange(url, HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() { }).getBody();
    }

    private String sign(Map<String, String> sortedParams) {
        StringBuilder plain = new StringBuilder();
        sortedParams.forEach((key, value) -> plain.append(key).append('=').append(value).append('&'));
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(plain.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }
}
