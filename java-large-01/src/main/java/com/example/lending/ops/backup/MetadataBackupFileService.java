package com.example.lending.ops.backup;

import com.example.lending.ops.common.StringUtils.Maps;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;
import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

/** Pulls metadata backup chunks (product catalogue, rate cards) from a peer node. Use MetadataBackupSyncService. */
@Service
@RequiredArgsConstructor
public class MetadataBackupFileService {

    static final String HTTP_VND_APACHE_KYLIN_V4_PUBLIC_JSON = "application/vnd.lending.ops-v1-public+json";

    private static final class JsonUtil {
        static byte[] writeValueAsBytes(Object value) throws JsonProcessingException { return new ObjectMapper().writeValueAsBytes(value); }
    }

    private final RestTemplate restTemplate =
            new RestTemplateBuilder().setConnectTimeout(Duration.ofSeconds(5)).setReadTimeout(Duration.ofMinutes(5)).build();
    @Value("${ops.backup.tmp-dir}")
    private final Path backupDir;

    public String downloadMetadataBackTmpFile(String tmpFilePath, Long tmpFileSize, String resourceGroupId,
            String fromHost) throws JsonProcessingException {
        val url = String.format(Locale.ROOT, "http://%s/kylin/api/system/metadata_backup_tmp_file", fromHost);
        val req = Maps.newHashMap();
        req.put("resource_group_id", resourceGroupId);
        req.put("tmp_file_path", tmpFilePath);
        req.put("tmp_file_size", tmpFileSize);
        val httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, HTTP_VND_APACHE_KYLIN_V4_PUBLIC_JSON);
        httpHeaders.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
        val httpEntity = new HttpEntity<>(JsonUtil.writeValueAsBytes(req), httpHeaders);
        RequestCallback requestCallback = restTemplate.httpEntityCallback(httpEntity);
        return restTemplate.execute(url, HttpMethod.POST, requestCallback, clientResponse -> {
            try (InputStream ins = clientResponse.getBody()) {
                return saveMetadataBackupTmpFromRequest(tmpFileSize, ins);
            }
        });
    }

    private String saveMetadataBackupTmpFromRequest(Long tmpFileSize, InputStream ins) throws IOException {
        Path target = Files.createTempFile(Files.createDirectories(backupDir), "metadata-", ".zip");
        if (Files.copy(ins, target, StandardCopyOption.REPLACE_EXISTING) != tmpFileSize) {
            Files.delete(target);
            throw new IOException("backup chunk truncated");
        }
        return target.toString();
    }
}
