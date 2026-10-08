package com.example.lending.loan.export;

import com.example.lending.loan.document.DocumentStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.nio.file.*;
import java.util.List;

@Component
public class ExportAttachmentStager {

    public record Attachments(List<String> files) {
    }

    private final DocumentStorage storage;
    private final String uploadTempDir;

    public ExportAttachmentStager(DocumentStorage storage,
                                  @Value("${documents.upload-temp-dir}") String uploadTempDir) {
        this.storage = storage;
        this.uploadTempDir = uploadTempDir;
    }

    public void stageAttachments(Long exportId, Attachments attachments) throws IOException {
        if (attachments.files() == null || attachments.files().isEmpty()) {
            return;
        }
        String stagingDir = storage.getRoot().resolve("staging").resolve(String.valueOf(exportId)).toString();
        for (String file : attachments.files()) {
            File localFile = new File(uploadTempDir, file);
            File stagedFile = new File(stagingDir, file);
            if (!localFile.exists() && !stagedFile.exists()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing file: " + file + ", please upload again");
            }
            if (localFile.exists()) {
                copyIfChanged(localFile, stagedFile.getAbsolutePath(), stagingDir);
            }
        }
    }

    private static void copyIfChanged(File source, String targetPath, String stagingDir) throws IOException {
        Files.createDirectories(Paths.get(stagingDir));
        Path target = Paths.get(targetPath);
        if (!Files.exists(target) || Files.size(target) != source.length()) {
            Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
