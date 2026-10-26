package com.example.lending.ops.part5.archive;

/** Object-storage credentials for the closed-loan document archive bucket. */
public record ArchiveStorageCredentials(String accessKeyId, String secretAccessKey, String region, String bucket) {

    @Override
    public String toString() {
        return "ArchiveStorageCredentials[accessKeyId=" + accessKeyId + ", region=" + region + ", bucket=" + bucket + "]";
    }
}
