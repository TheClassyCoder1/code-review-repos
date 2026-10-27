package com.example.lending.ops.part5.archive;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Production link signer: the key must come from the environment; there is no fallback. */
@Configuration
@Profile("!test")
public class ArchiveLinkSignerConfig {

    @Bean
    public ArchiveLinkSigner archiveLinkSigner(@Value("${OPS_ARCHIVE_LINK_KEY}") String key) {
        return new ArchiveLinkSigner(ArchiveCredentialGuard.requireUsable("OPS_ARCHIVE_LINK_KEY", key));
    }
}
