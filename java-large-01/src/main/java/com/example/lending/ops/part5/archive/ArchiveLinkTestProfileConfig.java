package com.example.lending.ops.part5.archive;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Link signer for the {@code test} profile used by integration tests and local
 * runs, so they start without archive secrets in the environment.
 */
@Configuration
@Profile("test")
public class ArchiveLinkTestProfileConfig {

    @Bean
    public ArchiveLinkSigner archiveLinkSigner(
            @Value("${OPS_ARCHIVE_LINK_KEY:local-test-archive-link-key}") String key) {
        return new ArchiveLinkSigner(key);
    }
}
