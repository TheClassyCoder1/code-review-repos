package com.example.lending.loan.archive;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class DocumentBundleController {

    private final BundleArchiveService bundleService;

    public DocumentBundleController(BundleArchiveService bundleService) {
        this.bundleService = bundleService;
    }

    @PostMapping("/api/v1/loans/{loanId}/documents/bundle")
    public ArchiveResponse uploadBundle(@PathVariable Long loanId, @RequestHeader("X-User-Id") Long userId,
                                        @RequestParam("file") MultipartFile file) throws IOException {
        return ArchiveResponse.ok(bundleService.importBundle(loanId, userId, file));
    }
}
