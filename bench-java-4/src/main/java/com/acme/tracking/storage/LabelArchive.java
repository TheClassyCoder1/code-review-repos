package com.acme.tracking.storage;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class LabelArchive {

    private final Path root;

    public LabelArchive(@ConfigProperty(name = "tracking.labels.root") String root) {
        this.root = Path.of(root);
    }

    public Path storeLabel(long shipmentId, InputStream pdf) throws IOException {
        Path directory = root.resolve(Long.toString(shipmentId % 1000));
        Files.createDirectories(directory);
        Path target = directory.resolve(shipmentId + ".pdf");
        Path temp = Files.createTempFile(directory, "label-", ".tmp");
        try (InputStream in = pdf) {
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
        }
        return Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private Path labelPath(long shipmentId) {
        return root.resolve(Long.toString(shipmentId % 1000)).resolve(shipmentId + ".pdf");
    }


    public InputStream openLabel(long shipmentId) throws IOException {
        Path label = labelPath(shipmentId);
        if (!Files.exists(label)) {
            throw new IOException("label not found for shipment " + shipmentId);
        }
        return Files.newInputStream(label);
    }

    public boolean hasLabel(long shipmentId) {
        return Files.exists(labelPath(shipmentId));
    }


    public String labelChecksum(long shipmentId) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        DigestInputStream in = new DigestInputStream(Files.newInputStream(labelPath(shipmentId)), digest);
        in.transferTo(OutputStream.nullOutputStream());
        return HexFormat.of().formatHex(digest.digest());
    }
}
