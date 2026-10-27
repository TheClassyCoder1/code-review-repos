package com.example.lending.loan.ops.tenants;

import com.example.lending.loan.support.crypto.Sha256Tool;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Service
public class TenantApiKeyService {

    public enum ApiKeyType { PUBLIC, PRIVATE }

    private final JdbcTemplate jdbcTemplate;

    public TenantApiKeyService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean validateApiKeyWithType(String tenantId, String apiKey, ApiKeyType type) {
        if (apiKey == null || type == null) {
            return false;
        }
        List<String> hashes = jdbcTemplate.queryForList(
                "SELECT key_hash FROM lending.tenant_api_keys WHERE tenant_id = ? AND key_type = ? AND revoked = FALSE",
                String.class, tenantId, type.name());
        byte[] presented = Sha256Tool.sha256(apiKey).getBytes(StandardCharsets.UTF_8);
        return hashes.stream().anyMatch(hash -> MessageDigest.isEqual(hash.getBytes(StandardCharsets.UTF_8), presented));
    }
}
