package com.example.lending.ops.partner.jwt;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtDetailsService {
    /** JWT settings of a partner app; keys are public JWK JSON objects. */
    @Data
    public static class AppsJwtDetails { private String id, signature, signatureKey, algorithm, algorithmKey; }

    private final JdbcTemplate jdbcTemplate;

    public AppsJwtDetails get(String appId, boolean cached) {
        return jdbcTemplate.query("SELECT * FROM partner_apps_jwt_details WHERE id = ?",
                new BeanPropertyRowMapper<>(AppsJwtDetails.class), appId).stream().findFirst().orElse(null);
    }
}
