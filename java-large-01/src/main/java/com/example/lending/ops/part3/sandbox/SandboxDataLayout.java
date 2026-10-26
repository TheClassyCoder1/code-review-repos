package com.example.lending.ops.part3.sandbox;

import java.io.*;
import java.nio.file.*;

/** Creates the on-disk layout the sandbox source reads, so its table descriptor directory always exists. */
final class SandboxDataLayout {
    private SandboxDataLayout() { }
    static void ensureTableDescDir(CsvSource.KylinConfig config) {
        Path metadata = Path.of(config.getMetadataUrl().getIdentifier());
        try {
            Files.createDirectories(Files.createDirectories(metadata).resolve("../data/tableDesc"));
        } catch (IOException e) {
            throw new UncheckedIOException("sandbox table descriptor directory unavailable under " + metadata, e);
        }
    }
}
