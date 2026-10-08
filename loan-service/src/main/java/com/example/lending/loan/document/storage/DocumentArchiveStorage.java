package com.example.lending.loan.document.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DocumentArchiveStorage {

    private static final Logger log = LoggerFactory.getLogger(DocumentArchiveStorage.class);

    static final String RESOURCE_UPLOAD_PATH = "document.archive.base-path";

    private final String resourceBaseAbsolutePath;

    public DocumentArchiveStorage(@Value("${" + RESOURCE_UPLOAD_PATH + ":/lending/archive}") String resourceBaseAbsolutePath) {
        this.resourceBaseAbsolutePath = resourceBaseAbsolutePath;
    }

    public String archiveKey(String loanReference, String fileName) {
        return getStorageBaseDirectory() + "/" + loanReference + "/" + fileName;
    }

    public String getStorageBaseDirectory() {
        // All directory should end with File.separator
        if (resourceBaseAbsolutePath.startsWith("/")) {
            log.warn("{} -> {} should not start with / in object storage", RESOURCE_UPLOAD_PATH,
                    resourceBaseAbsolutePath);
            return resourceBaseAbsolutePath.substring(1);
        }
        return getStorageBaseDirectory();
    }
}
