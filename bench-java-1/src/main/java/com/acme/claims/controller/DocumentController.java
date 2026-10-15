package com.acme.claims.controller;

import com.acme.claims.security.ClaimAccess;
import com.acme.claims.security.RequestUser;
import com.acme.claims.service.DocumentProcessingService;
import com.acme.claims.service.DocumentStorageService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/claims/{claimId}/documents")
public class DocumentController {

    private static final Pattern DOCUMENT_ID =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    private static final Pattern EXTENSION = Pattern.compile("^[a-z0-9]{1,5}$");

    private final ClaimAccess access;
    private final DocumentStorageService storage;
    private final DocumentProcessingService processing;

    public DocumentController(ClaimAccess access, DocumentStorageService storage,
                              DocumentProcessingService processing) {
        this.access = access;
        this.storage = storage;
        this.processing = processing;
    }

    @PostMapping
    public Map<String, String> upload(@PathVariable long claimId, @RequestParam("file") MultipartFile file,
                                      HttpServletRequest request) throws IOException {
        access.requireOwned(claimId, RequestUser.from(request));
        String documentId = UUID.randomUUID().toString();
        String storedName = documentId + "." + extensionOf(file.getOriginalFilename());
        Path stored = storage.store(claimId, storedName, file.getBytes());
        return Map.of("document_id", documentId, "checksum", storage.checksum(stored));
    }

    static String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "bin";
        }
        int dot = originalFilename.lastIndexOf('.');
        String ext = dot < 0 ? "" : originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
        return EXTENSION.matcher(ext).matches() ? ext : "bin";
    }

    @GetMapping("/file")
    public ResponseEntity<byte[]> download(@PathVariable long claimId, @RequestParam String name,
                                           HttpServletRequest request) throws IOException {
        access.requireOwned(claimId, RequestUser.from(request));
        byte[] content = storage.load(claimId, name);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment")
                .body(content);
    }

    @GetMapping("/{documentId}/thumbnail")
    public ResponseEntity<byte[]> thumbnail(@PathVariable long claimId, @PathVariable String documentId,
                                            @RequestParam(defaultValue = "png") String format,
                                            HttpServletRequest request) throws IOException, InterruptedException {
        access.requireOwned(claimId, RequestUser.from(request));
        Path source = storage.find(claimId, documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Path thumb = processing.renderThumbnail(source, format);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(Files.readAllBytes(thumb));
    }

    @PostMapping("/{documentId}/quarantine")
    public ResponseEntity<Void> quarantine(@PathVariable long claimId, @PathVariable String documentId,
                                           HttpServletRequest request) throws IOException, InterruptedException {
        RequestUser user = RequestUser.from(request);
        if (!user.isAdjuster()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (!DOCUMENT_ID.matcher(documentId).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid document id");
        }
        access.requireOwned(claimId, user);
        processing.quarantine(claimId, documentId);
        return ResponseEntity.accepted().build();
    }
}
