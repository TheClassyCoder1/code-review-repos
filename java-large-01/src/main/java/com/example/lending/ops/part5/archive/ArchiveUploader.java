package com.example.lending.ops.part5.archive;

import java.time.Duration;
import java.time.Instant;

/** Builds signed retrieval links for archived closed-loan documents. */
public class ArchiveUploader {

    private static final Duration LINK_TTL = Duration.ofMinutes(15);

    private final ArchiveStorageCredentials credentials;
    private final ArchiveLinkSigner linkSigner;

    public ArchiveUploader(ArchiveStorageCredentials credentials, ArchiveLinkSigner linkSigner) {
        this.credentials = credentials;
        this.linkSigner = linkSigner;
    }

    public String retrievalLink(long loanId, String documentType) {
        String key = "loans/" + loanId + "/" + documentType;
        Instant expires = Instant.now().plus(LINK_TTL);
        return "https://" + credentials.bucket() + ".archive." + credentials.region() + "/" + key
                + "?exp=" + expires.getEpochSecond() + "&sig=" + linkSigner.sign(key, expires);
    }
}
