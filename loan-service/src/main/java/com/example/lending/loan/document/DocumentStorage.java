package com.example.lending.loan.document;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class DocumentStorage {

    private final Path root;

    public DocumentStorage(@Value("${documents.root}") String root) {
        this.root = Paths.get(root).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(root.resolve("catalog"));
        Files.createDirectories(root.resolve("bundles"));
    }

    public Path getRoot() {
        return root;
    }
}
