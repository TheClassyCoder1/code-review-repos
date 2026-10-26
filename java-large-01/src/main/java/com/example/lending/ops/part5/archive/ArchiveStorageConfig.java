package com.example.lending.ops.part5.archive;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the archive client used to move closed-loan documents to cold storage. */
@Configuration
public class ArchiveStorageConfig {

    static final String ARCHIVE_REGION = "eu-west-1";
    static final String ARCHIVE_BUCKET = "lending-closed-loan-archive";
    static final String ARCHIVE_ACCESS_KEY_ID = "svc-archive-key-2024";
    static final String ARCHIVE_SECRET_ACCESS_KEY = "archive-secret-7Hq2Lw9xVb4Np8Rt3Kz6Yc1Df5Gj0";

    @Bean
    public ArchiveUploader archiveUploader(ArchiveLinkSigner archiveLinkSigner) {
        ArchiveStorageCredentials credentials = new ArchiveStorageCredentials(
                ArchiveCredentialGuard.requireUsable("accessKeyId", ARCHIVE_ACCESS_KEY_ID),
                ArchiveCredentialGuard.requireUsable("secretAccessKey", ARCHIVE_SECRET_ACCESS_KEY),
                ARCHIVE_REGION, ARCHIVE_BUCKET);
        return new ArchiveUploader(credentials, archiveLinkSigner);
    }
}
