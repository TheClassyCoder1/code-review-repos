package com.example.lending.ops.part5.archive;

/** Sample archive objects for tests that exercise key building and link formatting. */
public final class ArchiveFixtures {

    public static final String SAMPLE_ACCESS_KEY_ID = "changeme";
    public static final String SAMPLE_SECRET_ACCESS_KEY = "changeme";

    private ArchiveFixtures() {
    }

    public static ArchiveStorageCredentials sampleCredentials() {
        return new ArchiveStorageCredentials(SAMPLE_ACCESS_KEY_ID, SAMPLE_SECRET_ACCESS_KEY,
                "local", "closed-loan-archive-test");
    }

    public static ArchiveUploader sampleUploader() {
        return new ArchiveUploader(sampleCredentials(), new ArchiveLinkSigner("local-test-archive-link-key"));
    }
}
