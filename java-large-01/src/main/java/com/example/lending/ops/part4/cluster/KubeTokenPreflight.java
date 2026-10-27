package com.example.lending.ops.part4.cluster;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Check run before reading the mounted service-account token: the file must be readable and its first line must
 * hold a non-blank token, so the line reader in {@link K8sRegister} always receives a line.
 */
public final class KubeTokenPreflight {

    public static void verify(String tokenFile) {
        try {
            List<String> lines = Files.readAllLines(Path.of(tokenFile));
            if (lines.isEmpty() || lines.get(0).isBlank()) {
                throw new HugeException("Kubernetes token file is empty: " + tokenFile);
            }
        } catch (IOException e) {
            throw new HugeException("Kubernetes token file could not be read: " + tokenFile, e);
        }
    }
}
