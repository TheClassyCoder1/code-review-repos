package com.example.lending.loan.partner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class StatementSyncClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public StatementSyncClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String downloadStatementTmpFile(String tmpFilePath, Long tmpFileSize, String branchId,
            String fromHost) throws JsonProcessingException {
        String url = String.format(Locale.ROOT, "http://%s/api/v1/statements/backup-tmp-file", fromHost);
        Map<String, Object> req = new HashMap<>();
        req.put("branch_id", branchId);
        req.put("tmp_file_path", tmpFilePath);
        req.put("tmp_file_size", tmpFileSize);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
        HttpEntity<byte[]> httpEntity = new HttpEntity<>(objectMapper.writeValueAsBytes(req), httpHeaders);
        RequestCallback requestCallback = restTemplate.httpEntityCallback(httpEntity);
        return restTemplate.execute(url, HttpMethod.POST, requestCallback, clientResponse -> {
            try (InputStream ins = clientResponse.getBody()) {
                return saveStatementTmpFromRequest(tmpFileSize, ins);
            }
        });
    }

    private String saveStatementTmpFromRequest(Long tmpFileSize, InputStream ins) throws IOException {
        Path tmp = Files.createTempFile("statement-", ".tmp");
        long copied = Files.copy(ins, tmp, StandardCopyOption.REPLACE_EXISTING);
        if (tmpFileSize != null && copied != tmpFileSize) {
            Files.deleteIfExists(tmp);
            throw new IOException("Statement file size mismatch");
        }
        return tmp.toString();
    }
}
