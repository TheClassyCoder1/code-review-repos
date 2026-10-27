package com.acme.warehouse.web;

import com.acme.warehouse.importer.StockCsvImporter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/imports")
public class ImportController {

    private final StockCsvImporter importer;
    private final Path inboxDir;

    public ImportController(StockCsvImporter importer, @Value("${warehouse.import.inbox}") String inboxDir) {
        this.importer = importer;
        this.inboxDir = Path.of(inboxDir);
    }

    @PostMapping(value = "/bulk", consumes = "text/csv")
    public Map<String, Integer> bulkImport(HttpServletRequest request) throws IOException {
        String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int imported = 0;
        for (String line : body.split("\\R")) {
            if (line.isBlank() || importer.looksLikeHeader(line)) {
                continue;
            }
            importer.save(importer.parseLine(line));
            imported++;
        }
        return Map.of("imported", imported);
    }

    @PostMapping("/manifest")
    public Map<String, Integer> importManifest() throws IOException {
        int imported = 0;
        try (Stream<String> lines = importer.openManifest(inboxDir.resolve("manifest.csv"))) {
            for (String line : (Iterable<String>) lines::iterator) {
                importer.save(importer.parseLine(line));
                imported++;
            }
        }
        return Map.of("imported", imported);
    }

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Integer> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        byte[] content = file.getBytes();
        Path target = inboxDir.resolve(UUID.randomUUID() + ".csv");
        Files.write(target, content);
        return Map.of("imported", importer.importFile(target));
    }
}
