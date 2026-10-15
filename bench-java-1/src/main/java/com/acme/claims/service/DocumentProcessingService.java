package com.acme.claims.service;

import com.acme.claims.config.ClaimsProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class DocumentProcessingService {

    private final DocumentStorageService storage;
    private final Path thumbnailDir;
    private final Path quarantineDir;

    public DocumentProcessingService(DocumentStorageService storage, ClaimsProperties props) {
        this.storage = storage;
        this.thumbnailDir = Paths.get(props.thumbnailDir());
        this.quarantineDir = Paths.get(props.quarantineDir());
    }

    public Path renderThumbnail(Path source, String format) throws IOException, InterruptedException {
        Files.createDirectories(thumbnailDir);
        Path output = thumbnailDir.resolve(source.getFileName() + "." + format);
        Process process = new ProcessBuilder("sh", "-c",
                "convert '" + source + "[0]' -thumbnail 320x320 " + output)
                .redirectErrorStream(true)
                .start();
        runToCompletion(process, "thumbnail");
        return output;
    }

    private static void runToCompletion(Process process, String step) throws IOException, InterruptedException {
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException(step + " timed out");
        }
        if (process.exitValue() != 0) {
            throw new IOException(step + " failed with exit code " + process.exitValue());
        }
    }

    public boolean scanForMalware(Path file) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("clamdscan", "--no-summary", file.toString())
                .redirectErrorStream(true)
                .start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("malware scan timed out");
        }
        return process.exitValue() == 0;
    }

    public void quarantine(long claimId, String documentId) throws IOException, InterruptedException {
        Files.createDirectories(quarantineDir);
        Path dir = storage.claimDirectory(claimId);
        Process process = new ProcessBuilder("sh", "-c",
                "mv " + dir + "/" + documentId + ".* " + quarantineDir + "/")
                .redirectErrorStream(true)
                .start();
        runToCompletion(process, "quarantine");
    }
}
