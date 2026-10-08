package com.example.lending.loan.portal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;

/** Streams rendered loan documents from the document-render service to the borrower portal. */
@Service
public class DocumentProxyService {

    private final RestTemplate restTemplate;
    private final JdbcTemplate jdbc;

    public DocumentProxyService(RestTemplateBuilder builder, JdbcTemplate jdbc,
                                @Value("${archive.render-url}") String renderUrl) {
        this.restTemplate = builder.rootUri(renderUrl)
                .setConnectTimeout(Duration.ofSeconds(3)).setReadTimeout(Duration.ofSeconds(20)).build();
        this.jdbc = jdbc;
    }

    static Long callerId(HttpServletRequest request) {
        try {
            return Long.valueOf(request.getHeader("X-User-Id"));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    public void proxyStatementBatch(HttpServletRequest request, HttpServletResponse response, Long batchId)
            throws IOException {
        Long batch = jdbc.queryForList("SELECT id FROM lending.statement_batches WHERE id = ? AND owner_user_id = ?",
                Long.class, batchId, callerId(request)).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        proxy(response, "statement-batches/" + batch);
    }

    public void proxy(HttpServletResponse response, String path) throws IOException {
        ResponseEntity<byte[]> upstream = restTemplate.getForEntity("/render/" + path, byte[].class);
        response.setStatus(upstream.getStatusCode().value());
        if (upstream.getHeaders().getContentType() != null) {
            response.setContentType(upstream.getHeaders().getContentType().toString());
        }
        if (upstream.getBody() != null) {
            response.getOutputStream().write(upstream.getBody());
        }
    }
}
