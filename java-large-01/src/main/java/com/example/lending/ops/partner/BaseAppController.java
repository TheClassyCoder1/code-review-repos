package com.example.lending.ops.partner;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.*;
import org.springframework.security.crypto.encrypt.*;
import org.springframework.stereotype.Service;

public abstract class BaseAppController {
    protected final AppsService appsService;
    private final TextEncryptor secretEncryptor;

    protected BaseAppController(AppsService appsService, String secretKey, String salt) {
        this.appsService = appsService;
        this.secretEncryptor = Encryptors.delux(secretKey, salt);
    }

    /** Partner credentials are stored AES-GCM encrypted; decrypt them for the SSO adapters. */
    protected void decoderSecret(Apps application) {
        application.setCredentials(secretEncryptor.decrypt(application.getCredentials()));
        application.setSharedPassword(secretEncryptor.decrypt(application.getSharedPassword()));
    }

    @Service
    @RequiredArgsConstructor
    public static class AppsService {
        private final JdbcTemplate jdbcTemplate;

        public Apps get(String id) {
            return jdbcTemplate.query("SELECT * FROM partner_apps WHERE id = ?", new BeanPropertyRowMapper<>(Apps.class), id)
                    .stream().findFirst().orElse(null);
        }
    }
}
