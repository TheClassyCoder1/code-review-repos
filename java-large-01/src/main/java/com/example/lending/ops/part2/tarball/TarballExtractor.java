package com.example.lending.ops.part2.tarball;

import com.example.lending.ops.part2.tarball.TarEntryGuard.StackException;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

/** Unpacks the runtime bundles (JDK, scoring-engine binaries) that the ops agents install on worker hosts. */
@Slf4j
public final class TarballExtractor {

    /** One entry from the bundle reader: a link if {@code linkName} is set, a file if {@code content} is. */
    public record Entry(String name, String linkName, InputStream content) { }

    public static void extract(Iterable<Entry> entries, Path destination) throws IOException {
        TarEntryGuard guard = new TarEntryGuard(destination);
        for (Entry entry : entries) {
            Path path = guard.resolveEntry(entry.name());
            if (entry.linkName() != null) {
                guard.requireLinkTargetInside(path, entry.linkName());
                createSymbolicLink(path, entry.linkName());
                guard.verifyCreatedLink(path);
            } else if (entry.content() != null) {
                createDirectories(path.getParent());
                Files.copy(entry.content(), path, StandardCopyOption.REPLACE_EXISTING);
            } else {
                createDirectories(path);
            }
        }
    }

    private static void createSymbolicLink(Path linkPath, String targetName) {
        Path targetPath = linkPath.getParent().resolve(targetName).normalize();
        if (!targetPath.isAbsolute()) {
            targetPath = linkPath.getParent().resolve(targetPath).normalize();
        }

        createDirectories(linkPath.getParent());

        try {
            Files.createSymbolicLink(linkPath, targetPath);
        } catch (IOException e) {
            log.error("Failed to create symbolic link from {} to {}", linkPath, targetPath, e);
            throw new StackException(e);
        }
    }

    private static void createDirectories(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new StackException(e);
        }
    }
}
