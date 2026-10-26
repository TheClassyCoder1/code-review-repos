package com.example.lending.ops.part3.crypto;

import java.io.*;
import java.nio.file.*;
import java.security.cert.*;

/** Only entry point to the trust anchor files. Files are validated before they are opened: regular, readable, non-empty and small, so the raw read cannot fail part-way or allocate an oversized buffer. */
public final class TrustAnchorLoader {
    static final long MAX_ANCHOR_BYTES = 64 * 1024;
    private TrustAnchorLoader() { }
    public static X509Certificate load(Path anchorDir, String name) throws IOException, CertificateException {
        Path file = anchorDir.resolve(name).normalize();
        if (!file.startsWith(anchorDir) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(file)) {
            throw new IOException("trust anchor not available: " + name);
        }
        long size = Files.size(file);
        if (size == 0 || size > MAX_ANCHOR_BYTES) {
            throw new IOException("trust anchor has unexpected size: " + name);
        }
        byte[] der = CertCrypto.getBytesFromFile(file.toFile());
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(der));
    }
}
