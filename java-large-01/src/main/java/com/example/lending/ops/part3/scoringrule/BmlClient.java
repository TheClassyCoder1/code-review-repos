package com.example.lending.ops.part3.scoringrule;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.io.*;
import java.time.Duration;

/** Client of the store that versions scoring-rule scripts. Downloads are fully buffered (and size-capped) and the HTTP connection is released before returning, so the response stream holds no connection or file handle. */
@Component
public class BmlClient {
    private static final int MAX_SCRIPT_BYTES = 2 * 1024 * 1024;
    private final RestTemplate restTemplate =
            new RestTemplateBuilder().setConnectTimeout(Duration.ofSeconds(3)).setReadTimeout(Duration.ofSeconds(15)).build();
    private final String baseUrl;
    public BmlClient(@Value("${ops.scoring.bml-url:http://localhost:9106}") String baseUrl) { this.baseUrl = baseUrl; }
    public BmlDownloadResponse download(String user, String resourceId, String version) {
        byte[] content = restTemplate.getForObject(baseUrl + "/api/bml/download?resourceId={r}&version={v}&user={u}", byte[].class, resourceId, version, user);
        if (content == null || content.length > MAX_SCRIPT_BYTES) throw new IllegalStateException("bad scoring rule resource " + resourceId);
        return new BmlDownloadResponse(new ByteArrayInputStream(content));
    }
    public record BmlDownloadResponse(InputStream inputStream) { }
}
