package com.acme.claims.service;

import com.acme.claims.config.ClaimsProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class DocumentStorageService {

    private final Path root;

    public DocumentStorageService(ClaimsProperties props) {
        this.root = Paths.get(props.storageRoot()).toAbsolutePath().normalize();
    }

    public Path store(long claimId, String storedName, byte[] content) throws IOException {
        Path dir = claimDirectory(claimId);
        Files.createDirectories(dir);
        Path target = dir.resolve(storedName);
        Files.write(target, content, StandardOpenOption.CREATE_NEW);
        return target;
    }

    public Path claimDirectory(long claimId) {
        return root.resolve(Long.toString(claimId));
    }

    public List<String> listForClaim(long claimId) throws IOException {
        Path dir = claimDirectory(claimId);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    public byte[] load(long claimId, String fileName) throws IOException {
        Path file = claimDirectory(claimId).resolve(fileName);
        if (!Files.isRegularFile(file)) {
            throw new NoSuchFileException(fileName);
        }
        return Files.readAllBytes(file);
    }

    public Optional<Path> find(long claimId, String documentId) throws IOException {
        Path dir = claimDirectory(claimId);
        if (!Files.isDirectory(dir)) {
            return Optional.empty();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.getFileName().toString().startsWith(documentId + ".")).findFirst();
        }
    }

    public String checksum(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
