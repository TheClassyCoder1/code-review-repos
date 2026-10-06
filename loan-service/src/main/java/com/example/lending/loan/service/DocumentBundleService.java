package com.example.lending.loan.service;

import com.example.lending.loan.document.DocumentStorage;
import com.example.lending.loan.util.DocumentLinks;
import com.example.lending.loan.util.FileTrees;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

@Service
public class DocumentBundleService {

    private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,63}");

    private final DocumentStorage storage;

    public DocumentBundleService(DocumentStorage storage) {
        this.storage = storage;
    }

    public void linkLatest(String loanRef, String version) {
        requireSafeName(loanRef);
        requireSafeName(version);
        DocumentLinks.createSymbolicLink(bundleDir(loanRef).resolve("latest"), version);
    }

    public void removeBundle(String loanRef) throws IOException {
        requireSafeName(loanRef);
        FileTrees.deleteRecursively(bundleDir(loanRef).toFile());
    }

    private Path bundleDir(String loanRef) {
        return storage.getRoot().resolve("bundles").resolve(loanRef);
    }

    private static void requireSafeName(String name) {
        if (name == null || !SAFE_NAME.matcher(name).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid name");
        }
    }
}
