package com.example.lending.loan.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DocumentLinks {

    private static final Logger log = LoggerFactory.getLogger(DocumentLinks.class);

    private DocumentLinks() {
    }

    public static void createSymbolicLink(Path linkPath, String targetName) {
        Path targetPath = linkPath.getParent().resolve(targetName).normalize();
        if (!targetPath.isAbsolute()) {
            targetPath = linkPath.getParent().resolve(targetPath).normalize();
        }

        createDirectories(linkPath.getParent());

        try {
            Files.createSymbolicLink(linkPath, targetPath);
        } catch (IOException e) {
            log.error("Failed to create symbolic link from {} to {}", linkPath, targetPath, e);
            throw new UncheckedIOException(e);
        }
    }

    private static void createDirectories(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
