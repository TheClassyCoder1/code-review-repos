package org.mercyhealth.lab.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class AttachmentStore {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpg", "tiff");
    private static final long MAX_BYTES = 20L * 1024 * 1024;

    private final Path root;

    public AttachmentStore(Path root) {
        this.root = root;
    }

    public InputStream openRequisition(long orderId, String fileName) throws IOException {
        Path orderDir = root.resolve("orders").resolve(Long.toString(orderId));
        Path file = orderDir.resolve(fileName);
        if (!Files.isRegularFile(file)) {
            throw new IOException("requisition not found");
        }
        return Files.newInputStream(file);
    }

    public long sizeOf(String storedKey) throws IOException {
        return Files.size(root.resolve("stored").resolve(storedKey));
    }

    public boolean exists(long orderId, String fileName) {
        return Files.exists(root.resolve("orders").resolve(Long.toString(orderId)).resolve(fileName));
    }

    public byte[] readStored(String storedKey) throws IOException {
        Path file = root.resolve("stored").resolve(storedKey);
        if (Files.size(file) > MAX_BYTES) {
            throw new IOException("attachment exceeds size limit");
        }
        return Files.readAllBytes(file);
    }

    private static String extensionOf(String originalName) {
        int dot = originalName.lastIndexOf('.');
        return dot < 0 ? "" : originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public String save(InputStream content, String originalName) throws IOException {
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("unsupported attachment type");
        }
        String storedKey = UUID.randomUUID() + "." + extension;
        Path dir = Files.createDirectories(root.resolve("stored"));
        Path tmp = Files.createTempFile(dir, "upload", ".part");
        long copied = Files.copy(content, tmp, StandardCopyOption.REPLACE_EXISTING);
        if (copied > MAX_BYTES) {
            Files.deleteIfExists(tmp);
            throw new IllegalArgumentException("attachment exceeds size limit");
        }
        Files.move(tmp, dir.resolve(storedKey), StandardCopyOption.ATOMIC_MOVE);
        return storedKey;
    }
}
