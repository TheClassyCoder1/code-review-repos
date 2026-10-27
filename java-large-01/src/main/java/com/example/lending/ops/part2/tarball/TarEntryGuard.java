package com.example.lending.ops.part2.tarball;

import java.io.IOException;
import java.nio.file.*;

/**
 * Keeps everything a bundle creates inside its extraction root. Names and link targets are checked
 * lexically before anything is written; since that cannot see through links already on disk, the
 * entry's parent is resolved for real before writing and each new link is resolved for real after.
 */
public final class TarEntryGuard {

    public static class StackException extends RuntimeException {
        public StackException(String message) { super(message); }
        public StackException(Throwable cause) { super(cause); }
    }

    private final Path root;
    private final Path realRoot;

    public TarEntryGuard(Path root) throws IOException {
        this.root = Files.createDirectories(root).toAbsolutePath().normalize();
        this.realRoot = this.root.toRealPath();
    }

    public Path resolveEntry(String entryName) throws IOException {
        Path destination = root.resolve(entryName).normalize();
        if (entryName.isEmpty() || Path.of(entryName).isAbsolute() || !destination.startsWith(root) || destination.equals(root)) {
            throw new StackException("entry escapes extraction root: " + entryName);
        }
        Path existing = destination.getParent();
        while (!Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
            existing = existing.getParent();
        }
        if (!existing.toRealPath().startsWith(realRoot)) {
            throw new StackException("entry would be written through a link: " + entryName);
        }
        return destination;
    }

    /** A link target must be relative and stay under the root once resolved against the link's directory. */
    public void requireLinkTargetInside(Path linkPath, String targetName) {
        if (targetName.isEmpty() || Path.of(targetName).isAbsolute()
                || !linkPath.getParent().resolve(targetName).normalize().startsWith(root)) {
            throw new StackException("symbolic link escapes extraction root: " + linkPath);
        }
    }

    /** Follows a new link on disk; one that resolves outside the root is removed. */
    public void verifyCreatedLink(Path linkPath) throws IOException {
        if (Files.exists(linkPath) && !linkPath.toRealPath().startsWith(realRoot)) {
            Files.deleteIfExists(linkPath);
            throw new StackException("symbolic link resolves outside extraction root: " + linkPath);
        }
    }
}
