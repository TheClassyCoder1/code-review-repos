package com.example.lending.loan.controller;

import com.example.lending.loan.document.DocumentCatalog;
import com.example.lending.loan.service.DocumentBundleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentCatalog documentCatalog;
    private final DocumentBundleService documentBundleService;

    public DocumentController(DocumentCatalog documentCatalog, DocumentBundleService documentBundleService) {
        this.documentCatalog = documentCatalog;
        this.documentBundleService = documentBundleService;
    }

    @GetMapping("/{loanRef}")
    public List<String> listDocuments(@PathVariable String loanRef) {
        return documentCatalog.listDocuments(loanRef);
    }

    @PutMapping("/{loanRef}/latest")
    public ResponseEntity<Void> linkLatest(@PathVariable String loanRef, @RequestParam String version) {
        documentBundleService.linkLatest(loanRef, version);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{loanRef}/bundle")
    public ResponseEntity<Void> removeBundle(@PathVariable String loanRef) throws IOException {
        documentBundleService.removeBundle(loanRef);
        return ResponseEntity.noContent().build();
    }
}
